#!/bin/bash

# Luna2 Broker Auto - Setup completo e avvio per demo
# Script per inizializzare database, inserire dati demo e avviare app

set -e

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${CYAN}"
echo "======================================================================"
echo "🚗 Luna2 Broker Auto - Setup Demo Completo"
echo "======================================================================"
echo -e "${NC}"

# Configurazione database
DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="luna2"
DB_USER="luna2_user"
DB_PASSWORD="luna2_password"
DB_ROOT_PASSWORD="root"

# Funzione per verificare se MySQL è in esecuzione
check_mysql() {
    echo -e "${CYAN}🔍 Verifica MySQL in esecuzione...${NC}"
    
    if command -v mysql &> /dev/null; then
        if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e "SELECT 1" &> /dev/null; then
            echo -e "${GREEN}✅ MySQL è raggiungibile${NC}"
            return 0
        fi
    fi
    
    # Verifica via Docker
    if docker ps | grep -q "luna2-mysql\|mysql"; then
        echo -e "${GREEN}✅ MySQL container è in esecuzione${NC}"
        return 0
    fi
    
    echo -e "${YELLOW}⚠️  MySQL non trovato. Avvio Docker Compose...${NC}"
    return 1
}

# Funzione per avviare MySQL via Docker
start_mysql_docker() {
    echo -e "${CYAN}🐳 Avvio MySQL container...${NC}"
    
    if [ -f "docker-compose-preprod.yml" ]; then
        docker-compose -f docker-compose-preprod.yml up -d mysql
        
        echo -e "${YELLOW}⏳ Attendo che MySQL sia pronto (max 60 secondi)...${NC}"
        for i in {1..60}; do
            if docker exec luna2-mysql mysqladmin ping -h"localhost" -u"$DB_USER" -p"$DB_PASSWORD" --silent &> /dev/null; then
                echo -e "${GREEN}✅ MySQL è pronto${NC}"
                sleep 2
                return 0
            fi
            sleep 1
            echo -n "."
        done
        
        echo -e "${RED}❌ Timeout: MySQL non risponde${NC}"
        return 1
    else
        echo -e "${RED}❌ File docker-compose-preprod.yml non trovato${NC}"
        return 1
    fi
}

# Funzione per eseguire SQL script
execute_sql() {
    local sql_file=$1
    local description=$2
    
    echo -e "${CYAN}📝 Esecuzione: ${description}...${NC}"
    
    if [ ! -f "$sql_file" ]; then
        echo -e "${RED}❌ File $sql_file non trovato${NC}"
        return 1
    fi
    
    # Prova prima con MySQL client nativo
    if command -v mysql &> /dev/null; then
        if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$sql_file" 2>&1; then
            echo -e "${GREEN}✅ ${description} completato${NC}"
            return 0
        fi
    fi
    
    # Fallback su Docker
    if docker ps | grep -q "luna2-mysql"; then
        if docker exec -i luna2-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$sql_file" 2>&1; then
            echo -e "${GREEN}✅ ${description} completato (via Docker)${NC}"
            return 0
        fi
    fi
    
    echo -e "${RED}❌ Errore nell'esecuzione di ${description}${NC}"
    return 1
}

# Funzione per verificare tabelle create
verify_tables() {
    echo -e "${CYAN}🔍 Verifica tabelle Broker Auto...${NC}"
    
    local check_query="SELECT COUNT(*) as count FROM information_schema.tables 
                       WHERE table_schema = '$DB_NAME' 
                       AND table_name LIKE 'noleggio_%'"
    
    if command -v mysql &> /dev/null; then
        local count=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -sN -e "$check_query" "$DB_NAME" 2>/dev/null)
        
        if [ "$count" -ge 9 ]; then
            echo -e "${GREEN}✅ Trovate $count tabelle noleggio_*${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  Trovate solo $count tabelle (attese 9+)${NC}"
        fi
    fi
    
    # Fallback Docker
    if docker ps | grep -q "luna2-mysql"; then
        local count=$(docker exec luna2-mysql mysql -u"$DB_USER" -p"$DB_PASSWORD" -sN -e "$check_query" "$DB_NAME" 2>/dev/null)
        
        if [ "$count" -ge 9 ]; then
            echo -e "${GREEN}✅ Trovate $count tabelle noleggio_* (via Docker)${NC}"
            return 0
        fi
    fi
    
    return 1
}

# Funzione per verificare dati demo
verify_demo_data() {
    echo -e "${CYAN}🔍 Verifica dati demo inseriti...${NC}"
    
    local check_query="SELECT COUNT(*) FROM noleggio_lead"
    
    if command -v mysql &> /dev/null; then
        local count=$(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -sN -e "$check_query" "$DB_NAME" 2>/dev/null)
        
        if [ "$count" -ge 4 ]; then
            echo -e "${GREEN}✅ Trovati $count lead demo${NC}"
            
            # Mostra riepilogo per fase
            echo -e "${CYAN}📊 Distribuzione per fase:${NC}"
            mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" -e \
                "SELECT fase, COUNT(*) as count FROM noleggio_lead GROUP BY fase" "$DB_NAME" 2>/dev/null
            
            return 0
        else
            echo -e "${YELLOW}⚠️  Trovati solo $count lead (attesi 4+)${NC}"
        fi
    fi
    
    return 1
}

# Funzione per build Maven
build_app() {
    echo -e "${CYAN}🔨 Build applicazione Luna2...${NC}"
    
    if [ ! -f "pom.xml" ]; then
        echo -e "${RED}❌ File pom.xml non trovato${NC}"
        return 1
    fi
    
    echo -e "${YELLOW}⏳ Compilazione in corso (potrebbe richiedere qualche minuto)...${NC}"
    
    if mvn clean package -DskipTests -q; then
        echo -e "${GREEN}✅ Build completata${NC}"
        
        # Verifica WAR generato
        if [ -f "target/luna2-1.0.0.war" ] || [ -f "target/luna2.war" ]; then
            echo -e "${GREEN}✅ WAR file generato${NC}"
            return 0
        else
            echo -e "${YELLOW}⚠️  WAR file non trovato in target/${NC}"
        fi
    else
        echo -e "${RED}❌ Errore durante la build${NC}"
        return 1
    fi
}

# Funzione per avviare Tomcat
start_tomcat() {
    echo -e "${CYAN}🚀 Avvio Tomcat embedded...${NC}"
    
    # Controlla se Tomcat è già in esecuzione
    if lsof -Pi :8080 -sTCP:LISTEN -t >/dev/null 2>&1; then
        echo -e "${YELLOW}⚠️  Porta 8080 già in uso. Termino processo...${NC}"
        kill $(lsof -t -i:8080) 2>/dev/null || true
        sleep 2
    fi
    
    echo -e "${YELLOW}⏳ Avvio server su http://localhost:8080/luna2${NC}"
    
    # Avvia Tomcat con Maven plugin in background
    mvn tomcat7:run -Dmaven.tomcat.port=8080 > logs/tomcat.log 2>&1 &
    
    local tomcat_pid=$!
    echo "$tomcat_pid" > logs/tomcat.pid
    
    echo -e "${GREEN}✅ Tomcat avviato (PID: $tomcat_pid)${NC}"
    echo -e "${CYAN}📝 Log disponibili in: logs/tomcat.log${NC}"
    
    return 0
}

# ===================================
# MAIN EXECUTION
# ===================================

echo -e "${CYAN}Fase 1: Verifica prerequisiti${NC}"
echo "----------------------------------------------------------------------"

# Verifica MySQL
if ! check_mysql; then
    start_mysql_docker || {
        echo -e "${RED}❌ Impossibile avviare MySQL${NC}"
        exit 1
    }
fi

echo ""
echo -e "${CYAN}Fase 2: Setup Database${NC}"
echo "----------------------------------------------------------------------"

# Crea directory logs se non esiste
mkdir -p logs

# Esegui script SQL in sequenza
echo -e "${YELLOW}📝 Esecuzione script SQL in sequenza...${NC}"

# 1. Schema tabelle Broker Auto
if [ -f "database/luna2-noleggio-schema.sql" ]; then
    execute_sql "database/luna2-noleggio-schema.sql" "Schema tabelle Broker Auto"
else
    echo -e "${YELLOW}⚠️  File luna2-noleggio-schema.sql non trovato, provo a continuare...${NC}"
fi

# 2. Activation module
if [ -f "database/luna2-noleggio-activation.sql" ]; then
    execute_sql "database/luna2-noleggio-activation.sql" "Attivazione modulo Broker Auto"
fi

# 3. Demo data
if [ -f "database/luna2-noleggio-demo-data.sql" ]; then
    execute_sql "database/luna2-noleggio-demo-data.sql" "Dati demo (4 scenari)"
fi

# Verifica risultati
echo ""
verify_tables
verify_demo_data

echo ""
echo -e "${CYAN}Fase 3: Build Applicazione${NC}"
echo "----------------------------------------------------------------------"

build_app || {
    echo -e "${YELLOW}⚠️  Build fallita. Continuo comunque...${NC}"
}

echo ""
echo -e "${CYAN}Fase 4: Avvio Applicazione${NC}"
echo "----------------------------------------------------------------------"

start_tomcat

echo ""
echo -e "${GREEN}"
echo "======================================================================"
echo "✅ Setup completato!"
echo "======================================================================"
echo -e "${NC}"
echo ""
echo -e "${CYAN}🌐 Applicazione disponibile su:${NC}"
echo "   http://localhost:8080/luna2"
echo ""
echo -e "${CYAN}📝 Login di default:${NC}"
echo "   Username: admin"
echo "   Password: admin123"
echo ""
echo -e "${CYAN}🚗 Modulo Broker Auto:${NC}"
echo "   http://localhost:8080/luna2/app/noleggio/lead"
echo ""
echo -e "${CYAN}📊 Dati demo inseriti:${NC}"
echo "   - Mario Rossi (POST_VENDITA): 3 Fiat Ducato consegnati"
echo "   - Anna Ferrari (PREVENTIVAZIONE): VW Golf diesel"
echo "   - Lorenzo Verdi (ISTRUTTORIA): BMW 320d"
echo "   - Giulia Bianchi (ORDINE): 5 Audi A4"
echo "   - Sara Conti (NBT): Noleggio weekend"
echo ""
echo -e "${CYAN}📝 Comandi utili:${NC}"
echo "   Logs:     tail -f logs/tomcat.log"
echo "   Stop:     kill \$(cat logs/tomcat.pid)"
echo "   Restart:  ./broker-auto-demo-setup.sh"
echo ""
echo -e "${YELLOW}⏳ Attendi 20-30 secondi per il caricamento completo dell'app${NC}"
echo ""
echo -e "${GREEN}🎉 Buona demo!${NC}"
echo ""
