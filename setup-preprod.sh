#!/bin/bash

# ============================================================================
# Luna2 ERP - Pre-Production Setup Script
# Installs database, configures application, builds and deploys
# ============================================================================

set -e

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="luna2"
DB_USER="${DB_USER:-luna2_user}"
DB_PASSWORD="${DB_PASSWORD:-luna2_password}"
MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-root}"

# Application configuration
APP_PORT="${APP_PORT:-8080}"
JWT_SECRET="${JWT_SECRET:-ChangeThisSecretInProduction}"
JWT_EXPIRY="${JWT_EXPIRY:-60}"

# Deployment paths
WORKSPACE_DIR="/workspaces/Luna2"
LUNA2_API_DIR="$WORKSPACE_DIR/luna2-api"
LUNA2_ROOT_DIR="$WORKSPACE_DIR"

# ============================================================================
# FUNCTIONS
# ============================================================================

print_header() {
    echo -e "${BLUE}========================================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================================${NC}"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_step() {
    echo -e "${BLUE}▶ $1${NC}"
}

check_command() {
    if ! command -v $1 &> /dev/null; then
        print_error "$1 is not installed"
        return 1
    fi
    return 0
}

# ============================================================================
# PRE-FLIGHT CHECKS
# ============================================================================

print_header "LUNA2 PRE-PRODUCTION SETUP - PRE-FLIGHT CHECKS"

print_step "Checking required tools..."
check_command java || exit 1
check_command mvn || exit 1
check_command mysql || exit 1
print_success "All required tools found"

print_step "Checking Java version..."
java_version=$(java -version 2>&1 | grep 'version' | head -1)
print_success "Java found: $java_version"

print_step "Checking Maven version..."
mvn_version=$(mvn --version 2>&1 | head -1)
print_success "Maven found: $mvn_version"

# ============================================================================
# DATABASE SETUP
# ============================================================================

print_header "SETTING UP DATABASE"

print_step "Creating database '$DB_NAME' on $DB_HOST:$DB_PORT..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "root" -p"$MYSQL_ROOT_PASSWORD" \
    -e "CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" \
    2>/dev/null || {
    print_warning "Could not connect as root. Attempting with $DB_USER..."
}

print_step "Creating database user '$DB_USER'..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "root" -p"$MYSQL_ROOT_PASSWORD" \
    -e "CREATE USER IF NOT EXISTS '$DB_USER'@'%' IDENTIFIED BY '$DB_PASSWORD';" \
    -e "GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'%';" \
    -e "FLUSH PRIVILEGES;" \
    2>/dev/null || {
    print_warning "Could not create user. User may already exist."
}

print_step "Importing database schema..."
mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" \
    < "$WORKSPACE_DIR/database/luna2-complete-schema.sql" \
    || {
    print_error "Failed to import database schema"
    exit 1
}

print_success "Database initialized successfully"

# Verify database
RECORD_COUNT=$(mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" \
    -e "SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA='$DB_NAME';" \
    -sN)

print_success "Database contains $RECORD_COUNT tables"

# ============================================================================
# APPLICATION CONFIGURATION
# ============================================================================

print_header "CONFIGURING APPLICATION"

print_step "Configuring Luna2 Core (root project)..."
cat > "$LUNA2_ROOT_DIR/src/main/resources/application.properties" << EOF
# ============================================================
# Luna2 Core Service - Production Configuration
# ============================================================

# Server Configuration
server.port=8081
server.servlet.context-path=/
server.compression.enabled=true
server.compression.min-response-size=1024

# Database Configuration (Hibernate)
hibernate.connection.driver_class=com.mysql.cj.jdbc.Driver
hibernate.connection.url=jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
hibernate.connection.username=$DB_USER
hibernate.connection.password=$DB_PASSWORD
hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
hibernate.hbm2ddl.auto=validate
hibernate.generate_statistics=true
hibernate.jdbc.batch_size=20
hibernate.order_inserts=true
hibernate.order_updates=true

# Logging
logging.level.root=INFO
logging.level.it.zensoftware.luna2=DEBUG
logging.file.name=logs/luna2-core.log
logging.file.max-size=10MB
logging.file.max-history=10

# Email Configuration (Optional)
mail.smtp.host=smtp.gmail.com
mail.smtp.port=587
mail.smtp.user=your-email@gmail.com
mail.smtp.password=your-app-password
mail.from=noreply@luna2.local

# Calendar Configuration
calendar.encryption.secret=$JWT_SECRET
calendar.sync.interval-minutes=30

# Google Calendar OAuth (Optional - configure later)
google.calendar.client.id=
google.calendar.client.secret=
google.calendar.redirect.uri=http://localhost:8080/api/v1/calendario/oauth2/callback

EOF

print_success "Luna2 Core configured"

print_step "Configuring Luna2 API..."
cat > "$LUNA2_API_DIR/src/main/resources/application.properties" << EOF
# ============================================================
# Luna2 REST API - Production Configuration
# ============================================================

# Server Configuration
server.port=$APP_PORT
server.servlet.context-path=/
server.compression.enabled=true
server.compression.min-response-size=1024
server.error.include-message=always
server.error.include-binding-errors=always

# Spring Boot Configuration
spring.application.name=luna2-api
spring.profiles.active=production

# Database Configuration (Hibernate)
spring.datasource.url=jdbc:mysql://$DB_HOST:$DB_PORT/$DB_NAME?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=$DB_USER
spring.datasource.password=$DB_PASSWORD
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.idle-timeout=600000
spring.datasource.hikari.max-lifetime=1800000

# Hibernate Configuration
spring.jpa.database-platform=org.hibernate.dialect.MySQL8Dialect
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=20
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true

# JWT Configuration
jwt.secret=$JWT_SECRET
jwt.exp-minutes=$JWT_EXPIRY
jwt.issuer=luna2-api

# Calendar Configuration
calendar.encryption.secret=$JWT_SECRET
calendar.sync.interval-minutes=30
dashboard.poll.interval-ms=5000

# Logging
logging.level.root=INFO
logging.level.it.zensoftware.luna2.api=DEBUG
logging.level.org.springframework.web=INFO
logging.level.org.hibernate=WARN
logging.file.name=logs/luna2-api.log
logging.file.max-size=10MB
logging.file.max-history=10

# Management Endpoints
management.endpoints.web.exposure.include=health,info,metrics
management.endpoint.health.show-details=when-authorized
management.metrics.export.simple.enabled=true

# CORS Configuration (API Security)
server.servlet.cors.allowed-origins=http://localhost:3000,http://localhost:3001,http://localhost:8080
server.servlet.cors.allowed-methods=GET,POST,PUT,DELETE,OPTIONS
server.servlet.cors.allowed-headers=*
server.servlet.cors.allow-credentials=true
server.servlet.cors.max-age=3600

EOF

print_success "Luna2 API configured"

print_step "Creating logs directory..."
mkdir -p "$WORKSPACE_DIR/logs"
print_success "Logs directory created"

# ============================================================================
# BUILD PROJECT
# ============================================================================

print_header "BUILDING PROJECT"

print_step "Building Luna2 Core..."
cd "$LUNA2_ROOT_DIR"
mvn clean package -DskipTests -q || {
    print_error "Failed to build Luna2 Core"
    exit 1
}
print_success "Luna2 Core built successfully"

print_step "Building Luna2 API..."
cd "$LUNA2_API_DIR"
mvn clean package -DskipTests -q || {
    print_error "Failed to build Luna2 API"
    exit 1
}
print_success "Luna2 API built successfully"

# ============================================================================
# VERIFY ARTIFACTS
# ============================================================================

print_header "VERIFYING BUILD ARTIFACTS"

CORE_JAR="$LUNA2_ROOT_DIR/target/luna2.war"
API_JAR="$LUNA2_API_DIR/target/luna2-api-1.0.0.jar"

if [ -f "$API_JAR" ]; then
    print_success "Found REST API JAR: $API_JAR"
else
    print_error "REST API JAR not found: $API_JAR"
    exit 1
fi

# ============================================================================
# CREATE STARTUP SCRIPTS
# ============================================================================

print_header "CREATING STARTUP SCRIPTS"

print_step "Creating API startup script..."
cat > "$WORKSPACE_DIR/start-api.sh" << 'EOF'
#!/bin/bash
cd /workspaces/Luna2/luna2-api
java -Xmx1024m -Xms512m \
    -Dspring.profiles.active=production \
    -jar target/luna2-api-1.0.0.jar
EOF
chmod +x "$WORKSPACE_DIR/start-api.sh"
print_success "API startup script created: $WORKSPACE_DIR/start-api.sh"

print_step "Creating stop script..."
cat > "$WORKSPACE_DIR/stop.sh" << 'EOF'
#!/bin/bash
echo "Stopping Luna2 services..."
pkill -f "luna2-api" || true
echo "Services stopped"
EOF
chmod +x "$WORKSPACE_DIR/stop.sh"
print_success "Stop script created: $WORKSPACE_DIR/stop.sh"

# ============================================================================
# CREATE SYSTEMD SERVICE (OPTIONAL)
# ============================================================================

print_header "SYSTEMD SERVICE CONFIGURATION"

print_step "Creating systemd service file..."
cat > /tmp/luna2-api.service << EOF
[Unit]
Description=Luna2 REST API Service
After=network.target mysql.service
Wants=mysql.service

[Service]
Type=simple
User=luna2
WorkingDirectory=$LUNA2_API_DIR
ExecStart=/usr/bin/java -Xmx1024m -Xms512m -Dspring.profiles.active=production -jar target/luna2-api-1.0.0.jar
Restart=on-failure
RestartSec=10
Environment="JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64"

[Install]
WantedBy=multi-user.target
EOF

print_warning "Systemd service template created at: /tmp/luna2-api.service"
print_warning "To install (requires sudo):"
echo "  sudo cp /tmp/luna2-api.service /etc/systemd/system/"
echo "  sudo systemctl daemon-reload"
echo "  sudo systemctl enable luna2-api"
echo "  sudo systemctl start luna2-api"

# ============================================================================
# POST-SETUP SUMMARY
# ============================================================================

print_header "SETUP COMPLETE"

print_success "Luna2 ERP has been successfully configured for pre-production"

echo ""
echo -e "${BLUE}Database Information:${NC}"
echo "  Host: $DB_HOST:$DB_PORT"
echo "  Database: $DB_NAME"
echo "  User: $DB_USER"
echo "  Password: $DB_PASSWORD"

echo ""
echo -e "${BLUE}Application Information:${NC}"
echo "  REST API Port: $APP_PORT"
echo "  JWT Secret: $JWT_SECRET"
echo "  JWT Expiry: $JWT_EXPIRY minutes"

echo ""
echo -e "${BLUE}Default Credentials:${NC}"
echo "  Username: admin"
echo "  Password: Admin@123456"
echo "  Alternative: testuser / User@123456"

echo ""
echo -e "${BLUE}Quick Start:${NC}"
echo "  1. Start API Server:"
echo "     $WORKSPACE_DIR/start-api.sh"
echo ""
echo "  2. Wait for startup (check logs):"
echo "     tail -f logs/luna2-api.log"
echo ""
echo "  3. Test API:"
echo "     curl -X POST http://localhost:$APP_PORT/api/v1/auth/login \\"
echo "       -H 'Content-Type: application/json' \\"
echo "       -d '{\"username\":\"admin\",\"password\":\"Admin@123456\"}'"
echo ""
echo "  4. Stop Server:"
echo "     $WORKSPACE_DIR/stop.sh"

echo ""
echo -e "${BLUE}API Endpoints:${NC}"
echo "  POST   /api/v1/auth/login                    - Login & get JWT token"
echo "  GET    /api/v1/clienti                       - List customers"
echo "  GET    /api/v1/ordini                        - List orders"
echo "  GET    /api/v1/fatture                       - List invoices"
echo "  GET    /api/v1/calendario/events             - List calendar events"
echo "  GET    /api/v1/dashboard/stats               - Dashboard statistics"
echo "  POST   /api/v1/calendario/sync               - Sync calendars"

echo ""
echo -e "${BLUE}Important Notes:${NC}"
echo "  • Database: MySQL 8.0+ must be running on $DB_HOST:$DB_PORT"
echo "  • Logs: Check logs/luna2-api.log for errors"
echo "  • JWT Secret: Change '$JWT_SECRET' in production"
echo "  • Google Calendar: Configure OAuth credentials in application.properties"
echo "  • Backups: Regular database backups recommended"

echo ""
print_success "Setup Complete! Ready for deployment"
echo ""
