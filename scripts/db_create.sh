#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/../.env" ]; then
  source "$SCRIPT_DIR/../.env"
else
  echo "Error: .env file not found at $SCRIPT_DIR/../.env"
  exit 1
fi

SCHEMA_DIR="$SCRIPT_DIR/../schema"
DATA_DIR="$SCRIPT_DIR/../data"
PROC_DIR="$SCHEMA_DIR/stored_procedures"
VIEWS_DIR="$SCHEMA_DIR/views"
TABLE_DIR="$SCHEMA_DIR/tables"

DB_NAME="${DB_NAME:-sam}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"

echo "Creating DB after schema update"

# Validate all required directories exist
for dir in "$TABLE_DIR" "$DATA_DIR" "$PROC_DIR" "$VIEWS_DIR"; do
  if [ ! -d "$dir" ]; then
    echo "✗ Error: Directory $dir not found"
    exit 1
  fi
done

# Helper: run SQL files against the DB in order
MYSQL_CMD="mysql -h\"$DB_HOST\" -P\"$DB_PORT\" -u\"$DB_USER\" -p\"$DB_PASSWORD\""

run_sql_files() {
  local target_db="$1"
  shift
  for sql_file in "$@"; do
    if [ ! -f "$sql_file" ]; then
      echo "✗ Error: SQL file not found: $sql_file"
      exit 1
    fi
    echo "  → Loading $sql_file"
    if ! mysql \
      -h"$DB_HOST" \
      -P"$DB_PORT" \
      -u"$DB_USER" \
      -p"$DB_PASSWORD" \
      ${target_db:+-D"$target_db"} < "$sql_file"; then
      echo "✗ Error loading: $sql_file"
      exit 1
    fi
  done
}

# Drop and recreate database
echo "Dropping and recreating database '$DB_NAME'..."
if ! mysql \
  -h"$DB_HOST" \
  -P"$DB_PORT" \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  -e "DROP DATABASE IF EXISTS \`$DB_NAME\`; CREATE DATABASE \`$DB_NAME\`;"; then
  echo "✗ Error: Failed to create database"
  exit 1
fi

echo "Loading schema and data files..."

run_sql_files "$DB_NAME" \
  "$TABLE_DIR/customer.sql" \
  "$TABLE_DIR/account.sql" \
  "$TABLE_DIR/transaction.sql" \
  "$TABLE_DIR/alert_rule.sql" \
  "$TABLE_DIR/alert.sql" \
  "$TABLE_DIR/investigation.sql" \
  "$TABLE_DIR/watchlist.sql" \
  "$TABLE_DIR/watchlist_match.sql" \
  "$DATA_DIR/01_data_customers.sql" \
  "$DATA_DIR/02_data_accounts.sql" \
  "$DATA_DIR/03_data_transactions.sql" \
  "$DATA_DIR/04_data_alert_rules.sql" \
  "$DATA_DIR/05_data_alerts.sql" \
  "$DATA_DIR/06_data_investigations.sql" \
  "$DATA_DIR/07_data_watchlist.sql" \
  "$DATA_DIR/08_data_watchlist_matches.sql" \
  "$PROC_DIR/match_watchlist.sql" \
  "$PROC_DIR/raise_alert.sql" \
  "$PROC_DIR/screen_transaction.sql" \
  "$VIEWS_DIR/high_risk_accounts_vw.sql" \
  "$VIEWS_DIR/open_alerts_vw.sql"

echo "✓ Database '$DB_NAME' created and loaded successfully"