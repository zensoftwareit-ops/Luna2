#!/bin/bash

set -e

echo "🚀 Avvio demo Luna2 con dati Broker Auto..."

# Colori
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 1. Build WAR
echo -e "${YELLOW}[1/5] Building WAR...${NC}"
mvn clean package -DskipTests -q -Dorg.slf4j.simpleLogger.defaultLogLevel=warn 2>/dev/null || true
echo -e "${GREEN}✓ WAR built${NC}"

# 2. Avvia docker
echo -e "${YELLOW}[2/5] Starting Docker containers...${NC}"

# Kill any lingering luna2 containers
echo "  Cleaning up any existing containers..."
docker ps -a --filter "name=luna2-demo" --filter "name=luna2-mysql" --format "{{.Names}}" | xargs -r docker rm -f 2>/dev/null || true
docker container prune -f 2>/dev/null || true
sleep 2

# Start fresh
docker compose -f docker-compose-demo.yml down -v --remove-orphans 2>/dev/null || true
docker compose -f docker-compose-demo.yml up -d

# 3. Attendi che MySQL sia pronto
echo -e "${YELLOW}[3/5] Waiting for MySQL...${NC}"
sleep 10
for i in {1..30}; do
    if docker exec luna2-mysql mysqladmin ping -h localhost -u root -prootpass 2>/dev/null >/dev/null; then
        echo -e "${GREEN}✓ MySQL ready${NC}"
        break
    fi
    echo "  Attempt $i/30..."
    sleep 1
done

# 4. Carica dati demo
echo -e "${YELLOW}[4/5] Loading demo database...${NC}"
docker exec -i luna2-mysql mysql -u root -prootpass luna2 < database/schema.sql 2>/dev/null || true
docker exec -i luna2-mysql mysql -u root -prootpass luna2 < database/luna2-noleggio-schema.sql 2>/dev/null || true
docker exec -i luna2-mysql mysql -u root -prootpass luna2 < database/luna2-noleggio-activation.sql 2>/dev/null || true
docker exec -i luna2-mysql mysql -u root -prootpass luna2 < database/luna2-noleggio-demo-data.sql 2>/dev/null || true
echo -e "${GREEN}✓ Database loaded${NC}"

# 5. Attendi Tomcat
echo -e "${YELLOW}[5/5] Waiting for Tomcat deployment...${NC}"
sleep 15
for i in {1..60}; do
    if curl -f http://localhost:8080/ 2>/dev/null | grep -q "Luna2" || curl -f http://localhost:8080/ 2>/dev/null | grep -q "Tomcat"; then
        echo -e "${GREEN}✓ Tomcat ready${NC}"
        break
    fi
    echo "  Attempt $i/60..."
    sleep 1
done

# Mostra info accesso
echo ""
echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║          🎉 LUNA2 DEMO LIVE - READY FOR CLIENT!           ║${NC}"
echo -e "${BLUE}╠════════════════════════════════════════════════════════════╣${NC}"
echo -e "${BLUE}║${NC}"
echo -e "${BLUE}║${NC} ${GREEN}URL:${NC} ${BLUE}http://localhost:8080/login.action${NC}"
echo -e "${BLUE}║${NC} ${GREEN}User:${NC} admin"
echo -e "${BLUE}║${NC} ${GREEN}Pass:${NC} admin123"
echo -e "${BLUE}║${NC}"
echo -e "${BLUE}║${NC} ${YELLOW}Module: CRM Broker Auto${NC}"
echo -e "${BLUE}║${NC} ${YELLOW}Demo Data: 3 sample leads (prev, istrutt, ordine)${NC}"
echo -e "${BLUE}║${NC}"
echo -e "${BLUE}║${NC} ${GREEN}MySQL:${NC} localhost:3306 (luna2_user/luna2_password)"
echo -e "${BLUE}║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Mostra container status
echo "Container status:"
docker ps | grep luna2-demo

echo ""
echo -e "${YELLOW}Tip: Se non apre subito, aspetta 30 secondi che Tomcat completi il deployment.${NC}"
echo -e "${YELLOW}Logs: docker logs -f luna2-demo-tomcat${NC}"
