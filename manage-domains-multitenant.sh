#!/bin/bash

#################################################################################################
# MULTI-TENANT DOMAIN & CUSTOMER MANAGEMENT SYSTEM
# Gestisce multiple istanze di Luna2, ognuna con DB e UI PhpMyAdmin separati
#################################################################################################

set -e

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
WORKSPACE_DIR="${SCRIPT_DIR}"
DOMAINS_CONFIG="${SCRIPT_DIR}/domains-config.txt"
TEMPLATE_COMPOSE="${SCRIPT_DIR}/docker-compose-customer-template.yml"
NGINX_CONFIG_DIR="${SCRIPT_DIR}/docker/nginx/multi-tenant"
MAIN_COMPOSE="${SCRIPT_DIR}/docker-compose-main.yml"

# Variabili di default
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-Luna2Root@2024}"
MYSQL_USER_PASSWORD="${MYSQL_USER_PASSWORD:-Luna2User@2024}"
OAUTH_GOOGLE_CLIENT_ID="${OAUTH_GOOGLE_CLIENT_ID:-}"
OAUTH_GOOGLE_CLIENT_SECRET="${OAUTH_GOOGLE_CLIENT_SECRET:-}"

# Porte iniziali per allocazione
MYSQL_PORT_START=3307
APP_PORT_START=8081
PHPMYADMIN_PORT_START=8000

#################################################################################################
# UTILITY FUNCTIONS
#################################################################################################

log_info() {
    echo -e "\033[36m[INFO]\033[0m $1"
}

log_success() {
    echo -e "\033[32m[SUCCESS]\033[0m $1"
}

log_error() {
    echo -e "\033[31m[ERROR]\033[0m $1"
}

log_warn() {
    echo -e "\033[33m[WARN]\033[0m $1"
}

# Verifica prerequisiti
check_requirements() {
    log_info "Verificando requisiti..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker non installato"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose non installato"
        exit 1
    fi
    
    if ! command -v nginx &> /dev/null && docker ps 2>&1 | grep -q nginx; then
        log_info "Nginx running in Docker (OK)"
    fi
    
    if [ ! -f "$TEMPLATE_COMPOSE" ]; then
        log_error "Template file non trovato: $TEMPLATE_COMPOSE"
        exit 1
    fi
    
    log_success "Tutti i requisiti OK"
}

# Inizializza file di configurazione
init_config() {
    if [ ! -f "$DOMAINS_CONFIG" ]; then
        mkdir -p "$(dirname "$DOMAINS_CONFIG")"
        cat > "$DOMAINS_CONFIG" << 'EOF'
# Multi-Tenant Domain Configuration
# Format: DOMINIO|NOME_CLIENTE|PORTA_MYSQL|PORTA_APP|PORTA_PHPMYADMIN|CREATO|STATO
# Esempio: acme.com|ACME Inc|3307|8081|8000|2024-02-23|active
EOF
        log_success "File configurazione creato: $DOMAINS_CONFIG"
    fi
}

# Calcola prossima porta disponibile
get_next_port() {
    local port_list=$1
    local start_port=$2
    
    if [ -z "$port_list" ]; then
        echo $start_port
        return
    fi
    
    local max_used=$(echo "$port_list" | tr ' ' '\n' | sort -n | tail -1)
    echo $((max_used + 1))
}

# Alloca porte univoche per un nuovo cliente
allocate_ports() {
    local domain=$1
    
    # Estrai porte usate
    local mysql_ports=$(grep -v '^#' "$DOMAINS_CONFIG" | grep -v '^$' | cut -d'|' -f3 | sort -n)
    local app_ports=$(grep -v '^#' "$DOMAINS_CONFIG" | grep -v '^$' | cut -d'|' -f4 | sort -n)
    local pma_ports=$(grep -v '^#' "$DOMAINS_CONFIG" | grep -v '^$' | cut -d'|' -f5 | sort -n)
    
    local mysql_port=$(get_next_port "$mysql_ports" $MYSQL_PORT_START)
    local app_port=$(get_next_port "$app_ports" $APP_PORT_START)
    local pma_port=$(get_next_port "$pma_ports" $PHPMYADMIN_PORT_START)
    
    echo "$mysql_port|$app_port|$pma_port"
}

# Valida dominio
validate_domain() {
    local domain=$1
    
    if [[ ! "$domain" =~ ^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$ ]]; then
        log_error "Dominio non valido: $domain"
        return 1
    fi
    
    if grep -q "^$domain|" "$DOMAINS_CONFIG" 2>/dev/null; then
        log_error "Dominio già registrato: $domain"
        return 1
    fi
    
    return 0
}

# Cerea dominio nella config
find_domain_config() {
    local domain=$1
    grep "^$domain|" "$DOMAINS_CONFIG" 2>/dev/null || echo ""
}

#################################################################################################
# DOCKER COMPOSE MANAGEMENT
#################################################################################################

# Genera docker-compose per un cliente dal template
generate_docker_compose() {
    local domain=$1
    local customer_name=$2
    local mysql_port=$3
    local app_port=$4
    local pma_port=$5
    
    local output_file="${SCRIPT_DIR}/docker-compose-customer-${domain}.yml"
    
    log_info "Generando Docker Compose per $domain..."
    
    cp "$TEMPLATE_COMPOSE" "$output_file"
    
    # Sostituisci i placeholder
    sed -i "s|%%DOMAIN%%|${domain}|g" "$output_file"
    sed -i "s|%%CUSTOMER_NAME%%|${customer_name}|g" "$output_file"
    sed -i "s|%%MYSQL_PORT%%|${mysql_port}|g" "$output_file"
    sed -i "s|%%APP_PORT%%|${app_port}|g" "$output_file"
    sed -i "s|%%PHPMYADMIN_PORT%%|${pma_port}|g" "$output_file"
    sed -i "s|%%MYSQL_ROOT_PASSWORD%%|${MYSQL_ROOT_PASSWORD}|g" "$output_file"
    sed -i "s|%%MYSQL_USER_PASSWORD%%|${MYSQL_USER_PASSWORD}|g" "$output_file"
    sed -i "s|%%OAUTH_GOOGLE_CLIENT_ID%%|${OAUTH_GOOGLE_CLIENT_ID:-PLACEHOLDER}|g" "$output_file"
    sed -i "s|%%OAUTH_GOOGLE_CLIENT_SECRET%%|${OAUTH_GOOGLE_CLIENT_SECRET:-PLACEHOLDER}|g" "$output_file"
    
    log_success "Docker Compose generato: $output_file"
}

# Avvia container per cliente
start_customer() {
    local domain=$1
    
    local compose_file="${SCRIPT_DIR}/docker-compose-customer-${domain}.yml"
    
    if [ ! -f "$compose_file" ]; then
        log_error "File Docker Compose non trovato: $compose_file"
        return 1
    fi
    
    log_info "Avviando container per $domain..."
    
    docker-compose -f "$compose_file" up -d 2>&1 | tail -20
    
    # Aspetta che i container siano healthy
    sleep 5
    log_info "Aspettando health check..."
    for i in {1..30}; do
        if docker-compose -f "$compose_file" ps | grep -q "healthy\|running"; then
            log_success "Container $domain avviati e healthy"
            return 0
        fi
        sleep 2
    done
    
    log_warn "Health check timeout - container potrebbero non essere pronti"
    docker-compose -f "$compose_file" ps
}

# Ferma container per cliente
stop_customer() {
    local domain=$1
    
    local compose_file="${SCRIPT_DIR}/docker-compose-customer-${domain}.yml"
    
    if [ ! -f "$compose_file" ]; then
        log_error "File Docker Compose non trovato: $compose_file"
        return 1
    fi
    
    log_info "Fermando container per $domain..."
    docker-compose -f "$compose_file" down
    log_success "Container $domain fermati"
}

#################################################################################################
# NGINX CONFIGURATION
#################################################################################################

# Genera configurazione Nginx multi-tenant
generate_nginx_config() {
    local domain=$1
    local app_port=$2
    local pma_port=$3
    
    mkdir -p "$NGINX_CONFIG_DIR"
    
    local nginx_file="${NGINX_CONFIG_DIR}/${domain}.conf"
    
    log_info "Generando Nginx config per $domain..."
    
    cat > "$nginx_file" << EOF
# Nginx Configuration per cliente: $domain
# Generated: $(date)

# Upstream per Luna2 Application
upstream luna2_${domain//./\_} {
    server localhost:${app_port};
}

# Upstream per PhpMyAdmin
upstream phpmyadmin_${domain//./\_} {
    server localhost:${pma_port};
}

# HTTP -> HTTPS redirect
server {
    listen 80;
    server_name ${domain} www.${domain};
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }
    location / {
        return 301 https://\$server_name\$request_uri;
    }
}

# HTTPS - Luna2 Application
server {
    listen 443 ssl http2;
    server_name ${domain} www.${domain};

    ssl_certificate /etc/letsencrypt/live/${domain}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${domain}/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Security Headers
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Rate Limiting
    limit_req zone=general burst=100 nodelay;

    # Proxy Luna2
    location / {
        proxy_pass http://luna2_${domain//./\_};
        proxy_http_version 1.1;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 600s;
        proxy_connect_timeout 600s;
    }
}

# HTTPS - PhpMyAdmin Subdomain
server {
    listen 443 ssl http2;
    server_name phpmyadmin.${domain} pma.${domain};

    ssl_certificate /etc/letsencrypt/live/${domain}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${domain}/privkey.pem;

    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256;
    ssl_prefer_server_ciphers on;

    # Security Headers (stricter per admin interface)
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Basic Auth per PhpMyAdmin (optional, può essere abilitato)
    # auth_basic "PhpMyAdmin Access";
    # auth_basic_user_file /etc/nginx/.htpasswd-${domain};

    # Rate Limiting stricter
    limit_req zone=general burst=20 nodelay;

    # Proxy PhpMyAdmin
    location / {
        proxy_pass http://phpmyadmin_${domain//./\_};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}

# Redirect www -> non-www (optional)
server {
    listen 443 ssl http2;
    server_name www.${domain};
    ssl_certificate /etc/letsencrypt/live/${domain}/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/${domain}/privkey.pem;
    return 301 https://${domain}\$request_uri;
}
EOF
    log_success "Nginx config generata: $nginx_file"
}

# Ricarica Nginx
reload_nginx() {
    log_info "Ricaricando Nginx..."
    
    if docker ps --format "{{.Names}}" | grep -q "^nginx"; then
        docker exec nginx nginx -s reload
        log_success "Nginx ricaricato"
    else
        if command -v nginx &> /dev/null; then
            sudo nginx -s reload 2>/dev/null || {
                log_warn "Nginx non in esecuzione, saltato reload"
            }
        fi
    fi
}

#################################################################################################
# SSL/TLS MANAGEMENT
#################################################################################################

# Richiedi certificato SSL tramite Let's Encrypt
request_ssl() {
    local domain=$1
    
    log_info "Richiedendo certificato SSL per $domain..."
    
    if [ ! -d "/etc/letsencrypt/live/${domain}" ]; then
        if command -v certbot &> /dev/null; then
            certbot certonly \
                --non-interactive \
                --agree-tos \
                --email admin@${domain} \
                --webroot \
                --webroot-path=/var/www/certbot \
                -d "${domain}" \
                -d "www.${domain}" \
                -d "phpmyadmin.${domain}" \
                2>&1 | tail -20
            
            log_success "Certificato ottenuto per $domain"
        else
            log_warn "Certbot non disponibile - installare per SSL automatico"
        fi
    else
        log_warn "Certificato già esiste per $domain"
    fi
}

# Rinnova certificati SSL
renew_ssl() {
    local domain=$1
    
    log_info "Rinnovando certificato SSL per $domain..."
    
    if command -v certbot &> /dev/null; then
        certbot renew --quiet --cert-name "${domain}" 2>&1 | tail -10
        reload_nginx
        log_success "Certificato rinnovato per $domain"
    else
        log_error "Certbot non disponibile"
    fi
}

#################################################################################################
# MAIN COMMANDS
#################################################################################################

# Aggiungi nuovo cliente/dominio
cmd_add() {
    local domain=$1
    local customer_name=${2:-"Customer for $domain"}
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) add <dominio> [nome_cliente]"
        exit 1
    }
    
    validate_domain "$domain" || exit 1
    check_requirements
    init_config
    
    log_info "Aggiungendo nuovo cliente: $domain ($customer_name)"
    
    # Alloca porte
    local ports=$(allocate_ports "$domain")
    local mysql_port=$(echo "$ports" | cut -d'|' -f1)
    local app_port=$(echo "$ports" | cut -d'|' -f2)
    local pma_port=$(echo "$ports" | cut -d'|' -f3)
    
    log_info "Porte allocate - MySQL: $mysql_port, APP: $app_port, PhpMyAdmin: $pma_port"
    
    # Crea directory per dati
    mkdir -p "data/mysql-$domain" "data/phpmyadmin-$domain" "logs/luna2-$domain"
    
    # Genera docker-compose
    generate_docker_compose "$domain" "$customer_name" "$mysql_port" "$app_port" "$pma_port"
    
    # Genera nginx config
    generate_nginx_config "$domain" "$app_port" "$pma_port"
    
    # Registra nella configurazione
    local created=$(date '+%Y-%m-%d %H:%M:%S')
    echo "$domain|$customer_name|$mysql_port|$app_port|$pma_port|$created|active" >> "$DOMAINS_CONFIG"
    
    # Avvia container
    start_customer "$domain" || {
        log_error "Errore durante l'avvio dei container"
        exit 1
    }
    
    # Ricarica Nginx
    reload_nginx
    
    log_success "Cliente aggiunto completamente!"
    echo ""
    echo "┌─────────────────────────────────────────────────┐"
    echo "│ INFORMAZIONI CLIENTE                             │"
    echo "├─────────────────────────────────────────────────┤"
    echo "│ Dominio: $domain"
    echo "│ Cliente: $customer_name"
    echo "│ Port MySQL: $mysql_port (interno)"
    echo "│ Port App: $app_port"
    echo "│ Port PhpMyAdmin: $pma_port"
    echo "└─────────────────────────────────────────────────┘"
    echo ""
    echo "PROSSIMI PASSI:"
    echo "1. Aspetta 30s per health check"
    echo "2. Configura DNS: A record per $domain → IP server"
    echo "3. Richiedi SSL: $(basename $0) ssl $domain"
    echo "4. Accedi a:"
    echo "   - Luna2: https://$domain"
    echo "   - PhpMyAdmin: https://phpmyadmin.$domain"
    echo ""
}

# Rimuovi cliente/dominio
cmd_remove() {
    local domain=$1
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) remove <dominio>"
        exit 1
    }
    
    local config=$(find_domain_config "$domain")
    [ -z "$config" ] && {
        log_error "Dominio non trovato: $domain"
        exit 1
    }
    
    log_warn "Removing cliente: $domain (IRREVERSIBILE)"
    read -p "Digita '${domain}' per confermare: " confirm
    
    [ "$confirm" != "$domain" ] && {
        log_info "Operazione annullata"
        exit 0
    }
    
    # Ferma container
    stop_customer "$domain"
    
    # Rimuovi file docker-compose
    rm -f "${SCRIPT_DIR}/docker-compose-customer-${domain}.yml"
    
    # Rimuovi nginx config
    rm -f "${NGINX_CONFIG_DIR}/${domain}.conf"
    
    # Rimuovi dalla configurazione
    sed -i "/^${domain}|/d" "$DOMAINS_CONFIG"
    
    # Rimuovi dati (opzionale - chiedi conferma)
    read -p "Rimuovere anche i dati (DB, logs)? [y/N]: " remove_data
    if [[ "$remove_data" =~ ^[Yy]$ ]]; then
        rm -rf "data/mysql-$domain" "data/phpmyadmin-$domain" "logs/luna2-$domain"
        log_info "Dati rimossi"
    fi
    
    # Ricarica Nginx
    reload_nginx
    
    log_success "Cliente rimosso: $domain"
}

# Lista clienti
cmd_list() {
    init_config
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════════════════════════╗"
    echo "║ CLIENTI REGISTRATI                                                              ║"
    echo "╚════════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    
    local count=0
    while IFS='|' read -r domain customer mysql_port app_port pma_port created status; do
        [[ "$domain" =~ ^#.*$ ]] && continue
        [ -z "$domain" ] && continue
        
        ((count++))
        
        local status_icon="✓"
        [ "$status" != "active" ] && status_icon="✗"
        
        echo "[$status_icon] $domain"
        echo "    │ Client: $customer"
        echo "    │ Porte: MySQL=$mysql_port, App=$app_port, PhpMyAdmin=$pma_port"
        echo "    │ Creato: $created"
        
        # Verifica se container sono running
        if docker ps --format "{{.Names}}" | grep -q "luna2-${domain}"; then
            echo "    → Containers: RUNNING ✓"
        else
            echo "    → Containers: STOPPED ✗"
        fi
        echo ""
    done < "$DOMAINS_CONFIG"
    
    if [ $count -eq 0 ]; then
        echo "Nessun cliente registrato"
    else
        echo "────────────────────────────────────────────────────────────────────────────────"
        echo "Totale clienti: $count"
    fi
    echo ""
}

# Mostra mapping porte
cmd_ports() {
    init_config
    
    echo ""
    echo "╔════════════════════════════════════════════════════════════════════════════════╗"
    echo "║ PORT MAPPING                                                                     ║"
    echo "╚════════════════════════════════════════════════════════════════════════════════╝"
    echo ""
    printf "%-30s %-10s %-10s %-10s\n" "DOMINIO" "MYSQL" "APP" "PHPMYADMIN"
    echo "────────────────────────────────────────────────────────────────────────────────"
    
    while IFS='|' read -r domain _ mysql_port app_port pma_port _ _; do
        [[ "$domain" =~ ^#.*$ ]] && continue
        [ -z "$domain" ] && continue
        printf "%-30s %-10s %-10s %-10s\n" "$domain" "$mysql_port" "$app_port" "$pma_port"
    done < "$DOMAINS_CONFIG"
    echo ""
}

# Test connessione cliente
cmd_test() {
    local domain=$1
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) test <dominio>"
        exit 1
    }
    
    local config=$(find_domain_config "$domain")
    [ -z "$config" ] && {
        log_error "Dominio non trovato: $domain"
        exit 1
    }
    
    local mysql_port=$(echo "$config" | cut -d'|' -f3)
    local app_port=$(echo "$config" | cut -d'|' -f4)
    local pma_port=$(echo "$config" | cut -d'|' -f5)
    
    echo ""
    log_info "Testando cliente: $domain"
    echo ""
    
    # Test MySQL
    if nc -zv localhost "$mysql_port" 2>&1; then
        log_success "MySQL: OK (port $mysql_port)"
    else
        log_error "MySQL: FAILED (port $mysql_port)"
    fi
    
    # Test App
    if curl -s http://localhost:$app_port/healthz > /dev/null 2>&1; then
        log_success "Luna2 App: OK (port $app_port)"
    else
        log_error "Luna2 App: FAILED (port $app_port)"
    fi
    
    # Test PhpMyAdmin
    if curl -s http://localhost:$pma_port/ > /dev/null 2>&1; then
        log_success "PhpMyAdmin: OK (port $pma_port)"
    else
        log_error "PhpMyAdmin: FAILED (port $pma_port)"
    fi
    
    # Test containers
    echo ""
    log_info "Container health:"
    docker-compose -f "${SCRIPT_DIR}/docker-compose-customer-${domain}.yml" ps 2>/dev/null || log_warn "Docker Compose file not found"
    echo ""
}

# Richiedi SSL per un cliente
cmd_ssl() {
    local domain=$1
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) ssl <dominio>"
        exit 1
    }
    
    find_domain_config "$domain" > /dev/null || {
        log_error "Dominio non trovato: $domain"
        exit 1
    }
    
    request_ssl "$domain"
    reload_nginx
}

# Rinnova SSL per un cliente
cmd_renew() {
    local domain=$1
    
    if [ -z "$domain" ]; then
        log_info "Rinnovando tutti i certificati SSL..."
        while IFS='|' read -r domain _ _ _ _ _ _; do
            [[ "$domain" =~ ^#.*$ ]] && continue
            [ -z "$domain" ] && continue
            renew_ssl "$domain"
        done < "$DOMAINS_CONFIG"
    else
        find_domain_config "$domain" > /dev/null || {
            log_error "Dominio non trovato: $domain"
            exit 1
        }
        renew_ssl "$domain"
    fi
}

# Mostra logs di un cliente
cmd_logs() {
    local domain=$1
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) logs <dominio> [container]"
        echo "Container disponibili: luna2, mysql, phpmyadmin"
        exit 1
    }
    
    find_domain_config "$domain" > /dev/null || {
        log_error "Dominio non trovato: $domain"
        exit 1
    }
    
    local container=${2:-luna2}
    docker logs -f "${container}-${domain}" 2>/dev/null || log_error "Container non trovato: ${container}-${domain}"
}

# Status di un cliente
cmd_status() {
    local domain=$1
    
    [ -z "$domain" ] && {
        log_error "Uso: $(basename $0) status <dominio>"
        exit 1
    }
    
    local config=$(find_domain_config "$domain")
    [ -z "$config" ] && {
        log_error "Dominio non trovato: $domain"
        exit 1
    }
    
    echo ""
    log_info "Status per: $domain"
    echo ""
    
    local customer_name=$(echo "$config" | cut -d'|' -f2)
    echo "Cliente: $customer_name"
    echo ""
    
    echo "Container Status:"
    docker-compose -f "${SCRIPT_DIR}/docker-compose-customer-${domain}.yml" ps
    
    echo ""
    echo "Disk Usage:"
    du -sh data/mysql-$domain data/phpmyadmin-$domain logs/luna2-$domain 2>/dev/null | sed 's|^|  |'
    echo ""
}

#################################################################################################
# HELP
#################################################################################################

show_help() {
    cat << EOF

╔════════════════════════════════════════════════════════════════════════════════╗
║ MULTI-TENANT DOMAIN & CUSTOMER MANAGEMENT                                      ║
║ Luna2 Multi-Instance Management System                                          ║
╚════════════════════════════════════════════════════════════════════════════════╝

UTILIZZO: $(basename $0) <comando> [opzioni]

COMANDI:

  add <dominio> [nome_cliente]
    Aggiunge un nuovo cliente con istanza separata
    Es: $(basename $0) add acme.com "ACME Inc"
    
    • Crea container separati: Luna2, MySQL, PhpMyAdmin
    • Alloca porte univoche per il cliente
    • Genera nginx config
    • Avvia container

  remove <dominio>
    Rimuove un cliente (con conferma)
    Es: $(basename $0) remove acme.com
    
    • Ferma i container
    • Rimuove docker-compose file
    • Opzionalmente rimuove i dati

  list
    Lista tutti i clienti registrati
    Mostra: dominio, cliente, porte, status

  ports
    Mostra mapping porte per tutti i clienti
    Tabella: Dominio | MySQL | App | PhpMyAdmin

  test <dominio>
    Testa connessione ai servizi di un cliente
    • Verifica MySQL
    • Verifica Luna2 App
    • Verifica PhpMyAdmin

  ssl <dominio>
    Richiede certificato SSL Let's Encrypt
    Es: $(basename $0) ssl acme.com

  renew [dominio]
    Rinnova certificato SSL
    Senza dominio: rinnova tutti

  logs <dominio> [container]
    Mostra logs di un cliente
    Container: luna2, mysql, phpmyadmin

  status <dominio>
    Mostra status completo di un cliente
    • Container status
    • Disk usage

  help
    Mostra questo messaggio

VARIABILI DI AMBIENTE (opzionali):

  MYSQL_ROOT_PASSWORD
    Password root MySQL (default: Luna2Root@2024)

  MYSQL_USER_PASSWORD
    Password utente Luna2 (default: Luna2User@2024)

  OAUTH_GOOGLE_CLIENT_ID
    ID client Google OAuth (opzionale)

  OAUTH_GOOGLE_CLIENT_SECRET
    Secret Google OAuth (opzionale)

ESEMPI:

  # Aggiungere un nuovo cliente
  $(basename $0) add example.com "Example Corp"

  # Listare tutti i clienti
  $(basename $0) list

  # Testare un cliente
  $(basename $0) test example.com

  # Richiedere SSL per un cliente
  $(basename $0) ssl example.com

  # Rimuovere un cliente
  $(basename $0) remove example.com

STRUTTURA DIRECTORY:

  docker-compose-customer-DOMINIO.yml   # Docker Compose per cliente
  docker/nginx/multi-tenant/DOMINIO.conf # Nginx config per cliente
  data/mysql-DOMINIO/                   # Database cliente
  data/phpmyadmin-DOMINIO/              # PhpMyAdmin data
  logs/luna2-DOMINIO/                   # App logs

EOF
}

#################################################################################################
# MAIN
#################################################################################################

main() {
    local cmd="${1:-help}"
    
    case "$cmd" in
        add)
            cmd_add "${2:-}" "${3:-}"
            ;;
        remove|rm)
            cmd_remove "${2:-}"
            ;;
        list|ls)
            cmd_list
            ;;
        ports|port)
            cmd_ports
            ;;
        test)
            cmd_test "${2:-}"
            ;;
        ssl|certificate)
            cmd_ssl "${2:-}"
            ;;
        renew|renew-ssl)
            cmd_renew "${2:-}"
            ;;
        logs|log)
            cmd_logs "${2:-}" "${3:-}"
            ;;
        status)
            cmd_status "${2:-}"
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            log_error "Comando non riconosciuto: $cmd"
            echo "Usa '$(basename $0) help' per l'aiuto"
            exit 1
            ;;
    esac
}

main "$@"
