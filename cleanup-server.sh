#!/bin/bash

################################################################################
# Luna2 Server Cleanup Script
# Verifica e rimuove residui di istanze eliminate (Docker + Nginx)
# Esecuzione: bash cleanup-server.sh [--dry-run]
################################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DOMAINS_CONFIG="${SCRIPT_DIR}/domains-config.txt"
DRY_RUN="${1:-}"
COLORS_ON=true

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_success() { echo -e "${GREEN}[✓]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[⚠]${NC} $1"; }
log_error() { echo -e "${RED}[✗]${NC} $1"; }

# JSON output per il backend
json_array() { echo "[]"; }
json_add_item() { echo "$1" | /usr/bin/jq -e . >/dev/null 2>&1 && echo "$1" || echo "[]"; }

################################################################################
# RESULT COLLECTION
################################################################################

declare -a CLEANUP_RESULTS

add_result() {
    local type=$1   # orphaned_container | orphaned_nginx | failed_cleanup
    local name=$2   # container name o nginx file
    local status=$3 # success | skipped | error
    local message=$4
    
    local item=$(/usr/bin/jq -n \
        --arg type "$type" \
        --arg name "$name" \
        --arg status "$status" \
        --arg message "$message" \
        '{type: $type, name: $name, status: $status, message: $message}')
    
    CLEANUP_RESULTS+=("$item")
}

################################################################################
# DOCKER CLEANUP
################################################################################

cleanup_docker() {
    log_info "Scansione container Docker..."
    local orphaned=0
    local active_prefixes=()
    
    # Leggi i domini attivi da domains-config
    if [ -f "$DOMAINS_CONFIG" ]; then
        while IFS='|' read -r domain rest; do
            [[ "$domain" =~ ^#.*$ ]] && continue
            [ -z "$domain" ] && continue
            active_prefixes+=("$domain")
        done < "$DOMAINS_CONFIG"
    fi
    
    # Cercaall container Luna2
    local containers=$(docker ps -a --no-headers --filter "label=domain" --format "table {{.Names}}\t{{json .Labels}}" 2>/dev/null || true)
    
    if [ -z "$containers" ]; then
        log_info "Nessun container Luna2 trovato"
        return 0
    fi
    
    while IFS=$'\t' read -r name labels; do
        [ -z "$name" ] && continue
        
        # Estrai dominio da etichetta o da nome (luna2-DOMAIN, mysql-DOMAIN, ecc)
        local domain=$(echo "$labels" | /usr/bin/jq -r '.domain // empty' 2>/dev/null)
        if [ -z "$domain" ]; then
            domain=$(echo "$name" | grep -oE '[a-zA-Z0-9][a-zA-Z0-9\-]+(\.+[a-zA-Z0-9][a-zA-Z0-9\-]*)*' | head -1 || true)
        fi
        
        # Controlla se il dominio è ancora attivo
        local found=false
        for active in "${active_prefixes[@]}"; do
            if [[ "$name" == *"$active"* ]]; then
                found=true
                break
            fi
        done
        
        if [ "$found" = false ]; then
            log_warn "Container orfano trovato: $name"
            add_result "orphaned_container" "$name" "found" "Container senza dominio attivo"
            ((orphaned++))
            
            if [ "$DRY_RUN" != "--dry-run" ]; then
                if docker stop "$name" >/dev/null 2>&1 && docker rm "$name" >/dev/null 2>&1; then
                    log_success "Container rimosso: $name"
                    add_result "orphaned_container" "$name" "cleaned" "Container fermato e rimosso"
                else
                    log_error "Errore rimozione container: $name"
                    add_result "orphaned_container" "$name" "error" "Impossibile rimuovere il container"
                fi
            fi
        fi
    done <<< "$containers"
    
    if [ "$orphaned" -eq 0 ]; then
        log_success "Nessun container orfano trovato"
    else
        log_info "Container orfani trovati: $orphaned"
    fi
}

################################################################################
# NGINX CLEANUP
################################################################################

cleanup_nginx() {
    log_info "Scansione configurazioni Nginx..."
    local orphaned=0
    
    # Array di domini attivi
    local active_domains=()
    if [ -f "$DOMAINS_CONFIG" ]; then
        while IFS='|' read -r domain rest; do
            [[ "$domain" =~ ^#.*$ ]] && continue
            [ -z "$domain" ] && continue
            active_domains+=("$domain")
        done < "$DOMAINS_CONFIG"
    fi
    
    # Controlla /etc/nginx/conf.d
    if [ -d "/etc/nginx/conf.d" ]; then
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            local basename=$(basename "$file" .conf)
            
            # Estrai dominio dal nome file
            local found=false
            for domain in "${active_domains[@]}"; do
                if [[ "$basename" == "$domain" ]]; then
                    found=true
                    break
                fi
            done
            
            if [ "$found" = false ]; then
                # Verifica se è un symlink rotto
                if [ -L "$file" ] && [ ! -e "$file" ]; then
                    log_warn "Symlink nginx rotto: $file"
                    add_result "orphaned_nginx" "$file" "found" "Symlink a file inesistente"
                    
                    if [ "$DRY_RUN" != "--dry-run" ]; then
                        if rm "$file" 2>/dev/null; then
                            log_success "Symlink rimosso: $file"
                            add_result "orphaned_nginx" "$file" "cleaned" "Symlink rimosso"
                        else
                            log_error "Errore rimozione symlink: $file"
                            add_result "orphaned_nginx" "$file" "error" "Impossibile rimuovere il symlink"
                        fi
                    fi
                elif [ -f "$file" ]; then
                    log_warn "Config nginx orfana: $file (dominio non in config)"
                    add_result "orphaned_nginx" "$file" "found" "Dominio non in domains-config"
                fi
            fi
        done < <(find /etc/nginx/conf.d -name "*.conf" 2>/dev/null)
    fi
    
    # Controlla /etc/nginx/sites-enabled
    if [ -d "/etc/nginx/sites-enabled" ]; then
        while IFS= read -r file; do
            [ -z "$file" ] && continue
            local basename=$(basename "$file")
            
            local found=false
            for domain in "${active_domains[@]}"; do
                if [[ "$basename" == "$domain.conf" ]] || [[ "$basename" == "$domain" ]]; then
                    found=true
                    break
                fi
            done
            
            if [ "$found" = false ]; then
                if [ -L "$file" ] && [ ! -e "$file" ]; then
                    log_warn "Symlink sites-enabled rotto: $file"
                    add_result "orphaned_nginx" "$file" "found" "Symlink a file inesistente"
                    
                    if [ "$DRY_RUN" != "--dry-run" ]; then
                        if rm "$file" 2>/dev/null; then
                            log_success "Symlink rimosso: $file"
                            add_result "orphaned_nginx" "$file" "cleaned" "Symlink rimosso"
                        fi
                    fi
                fi
            fi
        done < <(find /etc/nginx/sites-enabled -type l 2>/dev/null)
    fi
    
    if [ "$orphaned" -eq 0 ]; then
        log_success "Nessun config nginx orfano trovato"
    else
        log_info "Config nginx orfani trovati: $orphaned"
    fi
}

################################################################################
# LETSENCRYPT CLEANUP
################################################################################

cleanup_letsencrypt() {
    log_info "Scansione certificati Let's Encrypt..."
    local orphaned=0
    
    local active_domains=()
    if [ -f "$DOMAINS_CONFIG" ]; then
        while IFS='|' read -r domain rest; do
            [[ "$domain" =~ ^#.*$ ]] && continue
            [ -z "$domain" ] && continue
            active_domains+=("$domain")
        done < "$DOMAINS_CONFIG"
    fi
    
    if [ -d "/etc/letsencrypt/live" ]; then
        for certdir in /etc/letsencrypt/live/*/; do
            [ ! -d "$certdir" ] && continue
            local domain=$(basename "$certdir")
            
            local found=false
            for active in "${active_domains[@]}"; do
                if [[ "$domain" == "$active" ]]; then
                    found=true
                    break
                fi
            done
            
            if [ "$found" = false ]; then
                log_warn "Certificato orfano: $domain"
                add_result "orphaned_cert" "$domain" "found" "Certificato per dominio non attivo"
                ((orphaned++))
                
                # Non rimuoviamo automaticamente i certificati per sicurezza
                if [ "$DRY_RUN" != "--dry-run" ]; then
                    log_info "  (I certificati orfani non vengono rimossi automaticamente per sicurezza)"
                    add_result "orphaned_cert" "$domain" "skipped" "Non rimosso per sicurezza"
                fi
            fi
        done
    fi
    
    if [ "$orphaned" -eq 0 ]; then
        log_success "Nessun certificato orfano trovato"
    fi
}

################################################################################
# NGINX CONFIG VALIDATION
################################################################################

validate_nginx() {
    log_info "Validazione configurazione Nginx..."
    
    if ! nginx -t 2>&1 | tail -5; then
        log_error "Nginx configuration non valida!"
        add_result "nginx_validation" "nginx" "error" "Configurazione Nginx non valida"
        return 1
    else
        log_success "Configurazione Nginx valida"
        add_result "nginx_validation" "nginx" "success" "Configurazione valida"
        
        # Ricarica se clean-up è stato eseguito
        if [ "$DRY_RUN" != "--dry-run" ]; then
            if systemctl reload nginx 2>/dev/null; then
                log_success "Nginx ricaricato"
                add_result "nginx_reload" "nginx" "success" "Nginx ricaricato con successo"
            fi
        fi
    fi
}

################################################################################
# SUMMARY
################################################################################

print_summary() {
    echo ""
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║  RIEPILOGO PULIZIA SERVER                                  ║"
    echo "╚════════════════════════════════════════════════════════════╝"
    
    if [ "$DRY_RUN" = "--dry-run" ]; then
        echo ""
        echo "🔍 MODALITÀ TEST (--dry-run): Nessuna modifica effettuata"
    fi
    
    echo ""
    local total=${#CLEANUP_RESULTS[@]}
    if [ "$total" -eq 0 ]; then
        echo "✨ Nessun problema trovato - Server pulito!"
    else
        echo "Risultati pulizia: $total elementi"
        echo ""
        
        for item in "${CLEANUP_RESULTS[@]}"; do
            local type=$(echo "$item" | /usr/bin/jq -r '.type')
            local name=$(echo "$item" | /usr/bin/jq -r '.name')
            local status=$(echo "$item" | /usr/bin/jq -r '.status')
            local message=$(echo "$item" | /usr/bin/jq -r '.message')
            
            case "$status" in
                success|cleaned)
                    echo "  ✓ [$type] $name - $message"
                    ;;
                error)
                    echo "  ✗ [$type] $name - $message"
                    ;;
                skipped|found)
                    echo "  ⚠ [$type] $name - $message"
                    ;;
            esac
        done
    fi
    echo ""
}

################################################################################
# OUTPUT JSON (for API)
################################################################################

output_json() {
    local result_array="["
    local first=true
    
    for item in "${CLEANUP_RESULTS[@]}"; do
        if [ "$first" = true ]; then
            result_array="${result_array}${item}"
            first=false
        else
            result_array="${result_array},${item}"
        fi
    done
    
    result_array="${result_array}]"
    
    /usr/bin/jq -n \
        --arg dryrun "$DRY_RUN" \
        --argjson results "$result_array" \
        '{dry_run: ($dryrun == "--dry-run"), results: $results, summary: {total: ($results | length), success: ([$results[] | select(.status == "success" or .status == "cleaned")] | length), errors: ([$results[] | select(.status == "error")] | length), warnings: ([$results[] | select(.status == "found" or .status == "skipped")] | length)}}'
}

################################################################################
# MAIN
################################################################################

main() {
    log_info "Inizio pulizia server"
    if [ "$DRY_RUN" = "--dry-run" ]; then
        log_info "Modalità TEST - nessuna modifica verrà effettuata"
    fi
    echo ""
    
    cleanup_docker
    echo ""
    cleanup_nginx
    echo ""
    cleanup_letsencrypt
    echo ""
    
    if ! validate_nginx; then
        log_error "Nginx configuration non valida - non ricaricare"
    fi
    
    print_summary
    
    # Output JSON per il pannello
    if [ "${OUTPUT_JSON:-false}" = "true" ]; then
        output_json
    fi
}

main
