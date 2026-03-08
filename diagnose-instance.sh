#!/bin/bash
# Quick diagnostic for instance routing issues

DOMAIN="${1:-5upemke9qv6ov6-app.gestionaleluna.it}"

echo "=== DIAGNOSTICA ISTANZA: $DOMAIN ==="
echo

echo "1. Container attivo?"
docker ps --filter "name=luna2-$DOMAIN" --format "{{.Names}} - {{.Status}}"
echo

echo "2. Config nginx generato?"
if [ -f "docker/nginx/multi-tenant/${DOMAIN}.conf" ]; then
    echo "✓ docker/nginx/multi-tenant/${DOMAIN}.conf esiste"
    head -n 20 "docker/nginx/multi-tenant/${DOMAIN}.conf"
else
    echo "✗ docker/nginx/multi-tenant/${DOMAIN}.conf NON ESISTE"
fi
echo

echo "3. Link host nginx?"
ls -l /etc/nginx/conf.d/${DOMAIN}.conf 2>/dev/null || echo "✗ Nessun link in conf.d"
ls -l /etc/nginx/sites-enabled/${DOMAIN}.conf 2>/dev/null || echo "✗ Nessun link in sites-enabled"
echo

echo "4. Certificato SSL?"
if [ -d "/etc/letsencrypt/live/$DOMAIN" ]; then
    echo "✓ Certificato esiste:"
    ls -l /etc/letsencrypt/live/$DOMAIN/
else
    echo "✗ Certificato NON ESISTE"
fi
echo

echo "5. Log nginx (ultimi 10 errori):"
tail -n 10 /var/log/nginx/error.log 2>/dev/null || echo "✗ Log non accessibile"
echo

echo "6. Test nginx config:"
nginx -t 2>&1
echo

echo "7. Test HTTP locale container:"
CONTAINER_IP=$(docker inspect -f '{{range.NetworkSettings.Networks}}{{.IPAddress}}{{end}}' luna2-$DOMAIN 2>/dev/null)
if [ -n "$CONTAINER_IP" ]; then
    echo "Container IP: $CONTAINER_IP"
    curl -s -I http://$CONTAINER_IP:8080/ | head -n 5
else
    echo "✗ Container non raggiungibile"
fi
echo

echo "8. Test HTTP via nginx host (porta 80):"
curl -H "Host: $DOMAIN" -s -I http://localhost/ | head -n 10
echo

echo "9. Configurazione nginx attiva per questo dominio:"
nginx -T 2>/dev/null | grep -A 30 "server_name.*$DOMAIN" | head -n 35
