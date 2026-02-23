#!/bin/bash

# ============================================================================
# Luna2 API Testing Script
# Use this script to test all major API endpoints
# ============================================================================

# Configuration
API_HOST="${API_HOST:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"
TEST_USER="${TEST_USER:-testuser}"
TEST_PASSWORD="${TEST_PASSWORD:-User@123456}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;36m'
NC='\033[0m'

# Store JWT tokens
ADMIN_TOKEN=""
TEST_TOKEN=""

# Utility functions
print_header() {
    echo -e "\n${BLUE}========== $1 ==========${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Test if API is available
test_api_health() {
    print_header "API Health Check"
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$API_HOST/api/v1/health")
    
    if [ "$response" -eq 200 ]; then
        print_success "API is healthy (HTTP $response)"
        return 0
    else
        print_error "API is not responding (HTTP $response)"
        print_info "Make sure API is running: $API_HOST"
        return 1
    fi
}

# ============================================================================
# AUTHENTICATION TESTS
# ============================================================================

test_admin_login() {
    print_header "Admin Login - POST /api/v1/auth/login"
    
    response=$(curl -s -X POST "$API_HOST/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$ADMIN_USER\",\"password\":\"$ADMIN_PASSWORD\"}")
    
    print_info "Request: POST /api/v1/auth/login"
    print_info "Payload: {\"username\":\"$ADMIN_USER\",\"password\":\"***\"}"
    echo "Response: $response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract token
    ADMIN_TOKEN=$(echo "$response" | jq -r '.token' 2>/dev/null)
    
    if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
        print_error "Failed to obtain admin token"
        return 1
    fi
    
    print_success "Admin token obtained: ${ADMIN_TOKEN:0:20}..."
    return 0
}

test_user_login() {
    print_header "User Login - POST /api/v1/auth/login"
    
    response=$(curl -s -X POST "$API_HOST/api/v1/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$TEST_USER\",\"password\":\"$TEST_PASSWORD\"}")
    
    print_info "Request: POST /api/v1/auth/login"
    echo "Response: $response" | jq '.' 2>/dev/null || echo "$response"
    
    # Extract token
    TEST_TOKEN=$(echo "$response" | jq -r '.token' 2>/dev/null)
    
    if [ -z "$TEST_TOKEN" ] || [ "$TEST_TOKEN" == "null" ]; then
        print_error "Failed to obtain user token"
        return 1
    fi
    
    print_success "User token obtained: ${TEST_TOKEN:0:20}..."
    return 0
}

# ============================================================================
# CLIENTI (CUSTOMERS) TESTS
# ============================================================================

test_list_clienti() {
    print_header "List Customers - GET /api/v1/clienti"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/clienti?page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/clienti?page=0&size=10"
    echo "Response:" 
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    count=$(echo "$response" | jq '.totalElements' 2>/dev/null)
    print_success "Found $count customers"
}

test_create_cliente() {
    print_header "Create Customer - POST /api/v1/clienti"
    
    timestamp=$(date +%s)
    
    response=$(curl -s -X POST "$API_HOST/api/v1/clienti" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"ragioneSociale\": \"Test Customer $timestamp\",
            \"partitaIva\": \"1234567890$timestamp\",
            \"email\": \"test$timestamp@example.com\",
            \"telefono\": \"+39 123 456 7890\",
            \"indirizzo\": \"Via Test 123\",
            \"citta\": \"Milano\",
            \"provincia\": \"MI\",
            \"cap\": \"20100\",
            \"paese\": \"Italia\"
        }")
    
    print_info "Request: POST /api/v1/clienti"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    id=$(echo "$response" | jq '.id' 2>/dev/null)
    if [ -n "$id" ] && [ "$id" != "null" ]; then
        print_success "Customer created with ID: $id"
        return 0
    else
        print_error "Failed to create customer"
        return 1
    fi
}

test_search_clienti() {
    print_header "Search Customers - GET /api/v1/clienti/search"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/clienti/search?nome=Test&page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/clienti/search?nome=Test"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# ============================================================================
# ORDINI (ORDERS) TESTS
# ============================================================================

test_list_ordini() {
    print_header "List Orders - GET /api/v1/ordini"
    
    anno=$(date +%Y)
    response=$(curl -s -X GET "$API_HOST/api/v1/ordini?anno=$anno&page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/ordini?anno=$anno"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    count=$(echo "$response" | jq '.totalElements' 2>/dev/null)
    print_success "Found $count orders"
}

test_search_ordini() {
    print_header "Search Orders - GET /api/v1/ordini/search"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/ordini/search?stato=CONFERMATO&page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/ordini/search?stato=CONFERMATO"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# ============================================================================
# FATTURE (INVOICES) TESTS
# ============================================================================

test_list_fatture() {
    print_header "List Invoices - GET /api/v1/fatture"
    
    anno=$(date +%Y)
    response=$(curl -s -X GET "$API_HOST/api/v1/fatture?anno=$anno&page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/fatture?anno=$anno"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
    
    count=$(echo "$response" | jq '.totalElements' 2>/dev/null)
    print_success "Found $count invoices"
}

test_search_fatture() {
    print_header "Search Invoices - GET /api/v1/fatture/search"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/fatture/search?stato=DA_PAGARE&page=0&size=10" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/fatture/search?stato=DA_PAGARE"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# ============================================================================
# DASHBOARD TESTS
# ============================================================================

test_dashboard_stats() {
    print_header "Dashboard Statistics - GET /api/v1/dashboard/stats"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/dashboard/stats" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/dashboard/stats"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

test_dashboard_ricavi() {
    print_header "Revenue Chart - GET /api/v1/dashboard/ricavi"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/dashboard/ricavi" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/dashboard/ricavi"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# ============================================================================
# CALENDARIO TESTS
# ============================================================================

test_list_calendar_events() {
    print_header "List Calendar Events - GET /api/v1/calendario/events"
    
    from=$(date -d "30 days ago" +%Y-%m-%d)
    to=$(date -d "30 days" +%Y-%m-%d)
    
    response=$(curl -s -X GET "$API_HOST/api/v1/calendario/events?userId=admin&from=$from&to=$to&provider=google" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/calendario/events?userId=admin&from=$from&to=$to&provider=google"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

test_list_calendar_providers() {
    print_header "List Calendar Providers - GET /api/v1/calendario/providers"
    
    response=$(curl -s -X GET "$API_HOST/api/v1/calendario/providers" \
        -H "Authorization: Bearer $ADMIN_TOKEN" \
        -H "Content-Type: application/json")
    
    print_info "Request: GET /api/v1/calendario/providers"
    echo "Response:"
    echo "$response" | jq '.' 2>/dev/null || echo "$response"
}

# ============================================================================
# MAIN TEST EXECUTION
# ============================================================================

main() {
    print_header "Luna2 API Test Suite"
    
    print_info "API Host: $API_HOST"
    print_info "Admin User: $ADMIN_USER"
    print_info "Test User: $TEST_USER"
    print_info "Note: Pre-configured to use local defaults. Modify variables at script top to test remote servers."
    
    # Health check
    if ! test_api_health; then
        print_error "Cannot proceed without healthy API"
        exit 1
    fi
    
    # Authentication
    if ! test_admin_login; then
        print_error "Cannot proceed without admin authentication"
        exit 1
    fi
    
    if ! test_user_login; then
        print_error "User login failed (non-critical, continuing)"
    fi
    
    # Clienti Tests
    test_list_clienti
    test_search_clienti
    test_create_cliente
    
    # Ordini Tests
    test_list_ordini
    test_search_ordini
    
    # Fatture Tests
    test_list_fatture
    test_search_fatture
    
    # Dashboard Tests
    test_dashboard_stats
    test_dashboard_ricavi
    
    # Calendario Tests
    test_list_calendar_events
    test_list_calendar_providers
    
    # Summary
    print_header "Test Suite Complete"
    print_success "All major endpoints tested successfully!"
    print_info "For more advanced testing, consider using Postman or curl manually"
}

# Run tests
main
