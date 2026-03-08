#!/bin/bash
# Test various endpoints on instance

DOMAIN="${1:-5upemke9qv6ov6-app.gestionaleluna.it}"
PORT=$(docker port luna2-$DOMAIN 8080/tcp 2>/dev/null | cut -d: -f2)

if [ -z "$PORT" ]; then
    echo "✗ Container non trovato"
    exit 1
fi

echo "=== TEST ENDPOINTS su porta $PORT ==="
echo

echo "1. Root /"
curl -s -I http://localhost:$PORT/ | head -n 8
echo

echo "2. /api/v1/info"
curl -s -I http://localhost:$PORT/api/v1/info | head -n 8
echo

echo "3. /healthz"
curl -s -I http://localhost:$PORT/healthz | head -n 8
echo

echo "4. /health"
curl -s -I http://localhost:$PORT/health | head -n 8
echo

echo "5. /actuator/health"
curl -s -I http://localhost:$PORT/actuator/health | head -n 8
echo

echo "6. Contenuto ROOT path:"
curl -s http://localhost:$PORT/ | head -n 30
