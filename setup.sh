#!/bin/bash

# Luna2 - Setup Script
# Questo script automatizza l'installazione e configurazione di Luna2

set -e

echo "======================================"
echo "   Luna2 - Setup Wizard"
echo "======================================"
echo ""

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check Java
echo -n "Verifica Java JDK 11+ ... "
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [ "$java_version" -ge 11 ]; then
        echo -e "${GREEN}OK${NC} ($(java -version 2>&1 | head -1))"
    else
        echo -e "${RED}ERRORE${NC}"
        echo "Java 11 o superiore richiesto. Versione trovata: $java_version"
        exit 1
    fi
else
    echo -e "${RED}ERRORE${NC}"
    echo "Java non trovato. Installare Java JDK 11 o superiore."
    exit 1
fi

# Check Maven
echo -n "Verifica Maven ... "
if command -v mvn &> /dev/null; then
    echo -e "${GREEN}OK${NC} ($(mvn -version | head -1))"
else
    echo -e "${RED}ERRORE${NC}"
    echo "Maven non trovato. Installare Apache Maven 3.6 o superiore."
    exit 1
fi

# Check MySQL
echo -n "Verifica MySQL ... "
if command -v mysql &> /dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${YELLOW}WARNING${NC}"
    echo "MySQL client non trovato. Assicurarsi che MySQL Server sia installato."
fi

echo ""
echo "======================================"
echo "   Configurazione Database"
echo "======================================"
echo ""

read -p "Host MySQL [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "Porta MySQL [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

read -p "Nome database [luna2]: " DB_NAME
DB_NAME=${DB_NAME:-luna2}

read -p "Username MySQL [luna2_user]: " DB_USER
DB_USER=${DB_USER:-luna2_user}

read -sp "Password MySQL: " DB_PASS
echo ""

# Test connessione MySQL
echo -n "Test connessione al database ... "
if mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e ";" 2>/dev/null; then
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${YELLOW}WARNING${NC}"
    echo "Impossibile connettersi. Verificare le credenziali."
    read -p "Continuare comunque? (y/n): " continue
    if [ "$continue" != "y" ]; then
        exit 1
    fi
fi

# Crea database se non esiste
echo -n "Creazione database ... "
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
echo -e "${GREEN}OK${NC}"

# Configura Hibernate
echo -n "Configurazione Hibernate ... "
if [ ! -f "src/main/resources/hibernate.cfg.xml" ]; then
    cp src/main/resources/hibernate.cfg.xml.template src/main/resources/hibernate.cfg.xml
    
    # Replace placeholders
    sed -i "s|localhost:3306/luna2|$DB_HOST:$DB_PORT/$DB_NAME|g" src/main/resources/hibernate.cfg.xml
    sed -i "s|<property name=\"hibernate.connection.username\">.*</property>|<property name=\"hibernate.connection.username\">$DB_USER</property>|g" src/main/resources/hibernate.cfg.xml
    sed -i "s|<property name=\"hibernate.connection.password\">.*</property>|<property name=\"hibernate.connection.password\">$DB_PASS</property>|g" src/main/resources/hibernate.cfg.xml
    
    echo -e "${GREEN}OK${NC}"
else
    echo -e "${YELLOW}SKIP${NC} (file già esistente)"
fi

# Importa schema database
echo ""
read -p "Importare lo schema del database? (y/n): " import_schema
if [ "$import_schema" = "y" ]; then
    echo -n "Importazione schema ... "
    mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" < src/main/resources/database/schema.sql
    echo -e "${GREEN}OK${NC}"
    echo -e "${GREEN}✓${NC} Utente admin creato (username: admin, password: admin123)"
fi

# Build del progetto
echo ""
echo "======================================"
echo "   Build Progetto"
echo "======================================"
echo ""

read -p "Compilare il progetto? (y/n): " build
if [ "$build" = "y" ]; then
    echo "Compilazione in corso..."
    mvn clean install -DskipTests
    echo -e "${GREEN}✓${NC} Build completata con successo"
fi

# Avvio applicazione
echo ""
echo "======================================"
echo "   Avvio Applicazione"
echo "======================================"
echo ""

read -p "Avviare l'applicazione con Jetty? (y/n): " start
if [ "$start" = "y" ]; then
    echo ""
    echo -e "${GREEN}======================================"
    echo "   Luna2 in avvio..."
    echo "======================================${NC}"
    echo ""
    echo "L'applicazione sarà disponibile su:"
    echo -e "${YELLOW}http://localhost:8080/luna2${NC}"
    echo ""
    echo "Credenziali di accesso:"
    echo -e "  Username: ${GREEN}admin${NC}"
    echo -e "  Password: ${GREEN}admin123${NC}"
    echo ""
    echo "Premi Ctrl+C per fermare il server"
    echo ""
    
    mvn jetty:run
else
    echo ""
    echo -e "${GREEN}======================================"
    echo "   Setup Completato!"
    echo "======================================${NC}"
    echo ""
    echo "Per avviare l'applicazione manualmente:"
    echo -e "  ${YELLOW}mvn jetty:run${NC}"
    echo ""
    echo "Oppure per creare il WAR:"
    echo -e "  ${YELLOW}mvn clean package${NC}"
    echo ""
    echo "L'applicazione sarà disponibile su:"
    echo -e "  ${YELLOW}http://localhost:8080/luna2${NC}"
    echo ""
    echo "Credenziali di accesso:"
    echo -e "  Username: ${GREEN}admin${NC}"
    echo -e "  Password: ${GREEN}admin123${NC}"
    echo ""
fi
