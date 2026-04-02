#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SCHEMA_FILE="${SCRIPT_DIR}/approval_workflow_schema.sql"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-luna2}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-}"

if ! command -v mysql >/dev/null 2>&1; then
  echo "Errore: comando mysql non trovato nel PATH"
  exit 1
fi

if [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "Errore: file schema non trovato: ${SCHEMA_FILE}"
  exit 1
fi

MYSQL_CMD=(mysql -h "${DB_HOST}" -P "${DB_PORT}" -u "${DB_USER}")
if [[ -n "${DB_PASS}" ]]; then
  MYSQL_CMD+=("-p${DB_PASS}")
fi

echo "Import schema approval workflow su ${DB_HOST}:${DB_PORT}/${DB_NAME}..."
"${MYSQL_CMD[@]}" "${DB_NAME}" < "${SCHEMA_FILE}"

echo "Verifica tabelle create..."
"${MYSQL_CMD[@]}" "${DB_NAME}" -e "SHOW TABLES LIKE 'approval_%';"

echo "Import completato con successo."
