#!/bin/bash
set -e

# Script di fix immediato per preprod
# Risolve: Welcome to nginx + SSL generation hang

echo "=================================================="
echo "FIX IMMEDIATO PREPROD - Luna2"
echo "=================================================="
echo ""

# 1. DIAGNOSTICA STATO ATTUALE
echo "📋 FASE 1: Diagnostica stato attuale"
echo "--------------------------------------------------"

echo "→ Controllando quale nginx è attivo..."
if docker ps | grep -q luna2-nginx; then
    NGINX_MODE="container"
    NGINX_NAME=$(docker ps --format '{{.Names}}' | grep nginx)
    echo "✓ Nginx in modalità CONTAINER: $NGINX_NAME"
elif systemctl is-active --quiet nginx 2>/dev/null; then
    NGINX_MODE="host"
    echo "✓ Nginx in modalità HOST (systemd)"
else
    echo "✗ ERRORE: Nessun nginx trovato!"
    exit 1
fi

echo ""
echo "→ Controllando directory certbot-webroot..."
if [ -d "./certbot-webroot/.well-known/acme-challenge" ]; then
    echo "✓ Directory ACME challenge presente"
else
    echo "⚠ Directory ACME challenge non presente - verrà creata"
fi

echo ""
echo "→ Controllando configurazioni dominio..."
DOMAIN="${1:-4upemke9qv6ov6-app.gestionaleluna.it}"
echo "  Dominio test: $DOMAIN"

if [ -f "docker/nginx/multi-tenant/${DOMAIN}.conf" ]; then
    echo "✓ Config in multi-tenant presente"
    grep -q "ssl_certificate" "docker/nginx/multi-tenant/${DOMAIN}.conf" && echo "  → Modalità: HTTPS" || echo "  → Modalità: HTTP"
else
    echo "⚠ Config non presente in multi-tenant"
fi

if [ "$NGINX_MODE" = "host" ]; then
    if [ -f "/etc/nginx/conf.d/${DOMAIN}.conf" ]; then
        echo "✓ Config in /etc/nginx/conf.d presente"
    else
        echo "✗ Config NON presente in /etc/nginx/conf.d (PROBLEMA!)"
    fi
fi

echo ""
echo ""

# Cleanup preventivo: evita doppio include stessa vhost in conf.d e sites-enabled
cleanup_duplicate_host_vhost() {
    local domain="$1"

    [ "$NGINX_MODE" != "host" ] && return 0

    if [ -e "/etc/nginx/conf.d/${domain}.conf" ] && [ -e "/etc/nginx/sites-enabled/${domain}.conf" ]; then
        echo "→ Trovata duplicazione vhost in conf.d + sites-enabled, mantengo solo conf.d..."
        rm -f "/etc/nginx/sites-enabled/${domain}.conf"
        echo "✓ Duplicazione rimossa"
    fi
}

# Corregge server_names_hash_* in /etc/nginx/nginx.conf quando ci sono molti vhost
tune_nginx_server_names_hash() {
    local nginx_conf="/etc/nginx/nginx.conf"

    [ ! -f "$nginx_conf" ] && {
        echo "✗ nginx.conf non trovato: $nginx_conf"
        return 1
    }

    if grep -qE '^[[:space:]]*server_names_hash_bucket_size[[:space:]]+' "$nginx_conf"; then
        sed -i -E 's|^[[:space:]]*server_names_hash_bucket_size[[:space:]]+[^;]+;|    server_names_hash_bucket_size 128;|' "$nginx_conf"
    else
        awk '
            {
                print
                if (!done && $0 ~ /^[[:space:]]*http[[:space:]]*\{[[:space:]]*$/) {
                    print "    server_names_hash_bucket_size 128;"
                    done=1
                }
            }
        ' "$nginx_conf" > "${nginx_conf}.tmp" && mv "${nginx_conf}.tmp" "$nginx_conf"
    fi

    if grep -qE '^[[:space:]]*server_names_hash_max_size[[:space:]]+' "$nginx_conf"; then
        sed -i -E 's|^[[:space:]]*server_names_hash_max_size[[:space:]]+[^;]+;|    server_names_hash_max_size 4096;|' "$nginx_conf"
    else
        awk '
            {
                print
                if (!done && $0 ~ /^[[:space:]]*http[[:space:]]*\{[[:space:]]*$/) {
                    print "    server_names_hash_max_size 4096;"
                    done=1
                }
            }
        ' "$nginx_conf" > "${nginx_conf}.tmp" && mv "${nginx_conf}.tmp" "$nginx_conf"
    fi

    echo "✓ Applicato tuning server_names_hash (bucket=128, max=4096)"
}

# 2. PULL CODICE AGGIORNATO
echo "📥 FASE 2: Aggiornamento codice da repository"
echo "--------------------------------------------------"
git fetch origin main
git reset --hard origin/main
echo "✓ Codice aggiornato a latest commit"

echo ""
echo ""

# 3. RESTART PANNELLO GESTIONE
echo "🔄 FASE 3: Restart server-manager"
echo "--------------------------------------------------"

if systemctl is-active --quiet luna2-server-manager 2>/dev/null; then
    echo "→ Riavvio via systemd..."
    systemctl restart luna2-server-manager
    sleep 2
    systemctl status luna2-server-manager --no-pager | head -5
    echo "✓ Pannello riavviato via systemd"
elif [ -f "server-manager/server-manager.pid" ]; then
    echo "→ Riavvio processo manuale..."
    PID=$(cat server-manager/server-manager.pid)
    kill $PID 2>/dev/null || true
    sleep 1
    cd server-manager
    nohup node server.js > /dev/null 2>&1 &
    echo $! > server-manager.pid
    cd ..
    echo "✓ Pannello riavviato manualmente"
else
    echo "⚠ Pannello non trovato - potrebbe essere già spento"
fi

echo ""
echo ""

# 4. PREPARAZIONE ENVIRONMENT NGINX
echo "🔧 FASE 4: Preparazione environment nginx"
echo "--------------------------------------------------"

# Crea directory ACME challenge
mkdir -p certbot-webroot/.well-known/acme-challenge
chmod -R 755 certbot-webroot
echo "test-acme-challenge" > certbot-webroot/.well-known/acme-challenge/test.txt

# Se modalità host, crea symlink
if [ "$NGINX_MODE" = "host" ]; then
    echo "→ Modalità HOST: preparo /var/www/certbot (senza symlink su /root)..."
    if [ -L "/var/www/certbot" ]; then
        rm -f /var/www/certbot
    fi
    if [ -L "/var/www/certbot/.well-known/acme-challenge" ]; then
        rm -f /var/www/certbot/.well-known/acme-challenge
    fi
    mkdir -p /var/www/certbot/.well-known/acme-challenge
    chmod 755 /var/www/certbot /var/www/certbot/.well-known /var/www/certbot/.well-known/acme-challenge
    echo "test-acme-challenge" > /var/www/certbot/.well-known/acme-challenge/test.txt
    chmod 644 /var/www/certbot/.well-known/acme-challenge/test.txt
    echo "✓ Webroot ACME pronto: /var/www/certbot"
    
    # Verifica directory multi-tenant
    mkdir -p docker/nginx/multi-tenant
    mkdir -p docker/nginx/vhosts
fi

echo "✓ Environment preparato"

echo ""
echo ""

# 5. RIGENERAZIONE CONFIGURAZIONE DOMINIO
echo "🌐 FASE 5: Rigenerazione configurazione dominio $DOMAIN"
echo "--------------------------------------------------"

# Forza rigenerazione in modalità HTTP (senza SSL)
echo "→ Generazione config HTTP-only per $DOMAIN..."

# Usa lo script aggiornato
bash manage-domains-multitenant.sh prepare-nginx

# In host mode rimuove eventuale duplicazione conf.d/sites-enabled per il dominio
cleanup_duplicate_host_vhost "$DOMAIN"

echo ""
echo ""

# 6. RELOAD NGINX
echo "♻️  FASE 6: Reload nginx"
echo "--------------------------------------------------"

if [ "$NGINX_MODE" = "container" ]; then
    echo "→ Reload container nginx..."
    docker exec "$NGINX_NAME" nginx -t
    docker exec "$NGINX_NAME" nginx -s reload
    echo "✓ Nginx container reloaded"
elif [ "$NGINX_MODE" = "host" ]; then
    echo "→ Reload host nginx..."
    nginx -t || {
        echo "⚠ nginx -t fallito, provo cleanup duplicati e ritento..."
        cleanup_duplicate_host_vhost "$DOMAIN"
        if ! nginx -t; then
            echo "⚠ Ancora fallito: provo tuning server_names_hash..."
            tune_nginx_server_names_hash
            nginx -t
        fi
    }
    systemctl reload nginx
    echo "✓ Nginx host reloaded"
fi

echo ""
echo ""

# 7. TEST ACME CHALLENGE
echo "🧪 FASE 7: Test ACME challenge"
echo "--------------------------------------------------"

sleep 2
echo "→ Testing http://$DOMAIN/.well-known/acme-challenge/test.txt"
RESPONSE=$(curl -sL -w "%{http_code}" "http://$DOMAIN/.well-known/acme-challenge/test.txt" -o /tmp/acme-test.txt 2>&1 || echo "000")

if [ "$RESPONSE" = "200" ]; then
    CONTENT=$(cat /tmp/acme-test.txt)
    if [ "$CONTENT" = "test-acme-challenge" ]; then
        echo "✅ ACME challenge FUNZIONANTE!"
    else
        echo "⚠️  ACME challenge risponde ma contenuto errato: $CONTENT"
    fi
else
    echo "❌ ACME challenge NON funzionante (HTTP $RESPONSE)"
    echo "   Questo impedirà la generazione SSL"
    if [ "$NGINX_MODE" = "host" ]; then
        echo "   Debug rapido host webroot:"
        ls -ld /var/www/certbot /var/www/certbot/.well-known /var/www/certbot/.well-known/acme-challenge 2>/dev/null || true
        ls -l /var/www/certbot/.well-known/acme-challenge/test.txt 2>/dev/null || true
    fi
fi

echo ""
echo ""

# 8. TEST DOMINIO
echo "🌍 FASE 8: Test accesso dominio"
echo "--------------------------------------------------"

echo "→ Testing http://$DOMAIN/"
RESPONSE=$(curl -sL -w "%{http_code}" "http://$DOMAIN/" -o /tmp/domain-test.txt 2>&1 || echo "000")

if [ "$RESPONSE" = "200" ]; then
    if grep -qi "welcome to nginx" /tmp/domain-test.txt; then
        echo "❌ PROBLEMA: Ancora 'Welcome to nginx'"
        echo "   Possibili cause:"
        echo "   - Configurazione non caricata correttamente"
        echo "   - Nginx cache"
        echo "   - Container backend non raggiungibile"
    else
        echo "✅ Dominio risponde correttamente (non è più Welcome to nginx)"
    fi
else
    echo "⚠️  Dominio non risponde (HTTP $RESPONSE)"
fi

echo ""
echo ""

# 9. SUMMARY E NEXT STEPS
echo "=================================================="
echo "SUMMARY"
echo "=================================================="
echo ""
echo "Nginx Mode: $NGINX_MODE"
echo "Domain: $DOMAIN"
echo ""
echo "NEXT STEPS:"
echo ""
echo "1. Verifica che il dominio non mostri più 'Welcome to nginx':"
echo "   curl http://$DOMAIN/"
echo ""
echo "2. Se il test ACME è OK, genera il certificato SSL:"
echo "   bash manage-domains-multitenant.sh ssl $DOMAIN"
echo ""
echo "3. Oppure usa il pannello web per generare SSL"
echo ""
echo "4. Se ci sono ancora problemi, controlla i log:"
echo "   journalctl -u nginx -n 50"
echo "   docker logs $NGINX_NAME (se container)"
echo ""
echo "=================================================="
