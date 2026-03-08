#!/bin/bash
# Check application deployment inside container

DOMAIN="${1:-5upemke9qv6ov6-app.gestionaleluna.it}"
CONTAINER="luna2-$DOMAIN"

echo "=== VERIFICA APPLICAZIONE CONTAINER: $CONTAINER ==="
echo

echo "1. Container status:"
docker ps --filter "name=$CONTAINER" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
echo

echo "2. Webapps directory:"
docker exec $CONTAINER ls -lh /usr/local/tomcat/webapps/ 2>/dev/null || echo "✗ Non posso accedere al container"
echo

echo "3. ROOT webapp presente?"
docker exec $CONTAINER ls -lh /usr/local/tomcat/webapps/ROOT/ 2>/dev/null | head -n 10 || echo "✗ ROOT non esiste"
echo

echo "4. Tomcat logs (ultimi 30 righe):"
docker exec $CONTAINER tail -n 30 /usr/local/tomcat/logs/catalina.out 2>/dev/null || echo "✗ Log non accessibile"
echo

echo "5. Test HTTP diretto su porta esposta:"
EXPOSED_PORT=$(docker port $CONTAINER 8080/tcp 2>/dev/null | cut -d: -f2)
if [ -n "$EXPOSED_PORT" ]; then
    echo "Porta esposta: $EXPOSED_PORT"
    curl -s -I http://localhost:$EXPOSED_PORT/ | head -n 8
else
    echo "✗ Nessuna porta esposta"
fi
echo

echo "6. Healthcheck logs:"
docker inspect $CONTAINER --format '{{range .State.Health.Log}}{{.Output}}{{end}}' 2>/dev/null | tail -n 20 || echo "✗ No healthcheck logs"
echo

echo "7. Container environment (DB config):"
docker exec $CONTAINER env | grep -E "DB_|MYSQL_" 2>/dev/null || echo "✗ No DB env vars"
