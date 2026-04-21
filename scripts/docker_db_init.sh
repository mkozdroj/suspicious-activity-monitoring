#!/bin/bash
set -e

TABLE_DIR="/docker-entrypoint-initdb.d/schema/tables"
DATA_DIR="/docker-entrypoint-initdb.d/data"

run_sql() {
  mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < "$1"
}

echo "Loading tables..."
run_sql "$TABLE_DIR/customer.sql"
run_sql "$TABLE_DIR/account.sql"
run_sql "$TABLE_DIR/txn.sql"
run_sql "$TABLE_DIR/alert_rule.sql"
run_sql "$TABLE_DIR/alert.sql"
run_sql "$TABLE_DIR/investigation.sql"
run_sql "$TABLE_DIR/watchlist.sql"
run_sql "$TABLE_DIR/watchlist_match.sql"

echo "Loading data..."
run_sql "$DATA_DIR/01_data_customers.sql"
run_sql "$DATA_DIR/02_data_accounts.sql"
run_sql "$DATA_DIR/03_data_transactions.sql"
run_sql "$DATA_DIR/04_data_alert_rules.sql"
run_sql "$DATA_DIR/05_data_alerts.sql"
run_sql "$DATA_DIR/06_data_investigations.sql"
run_sql "$DATA_DIR/07_data_watchlist.sql"
run_sql "$DATA_DIR/08_data_watchlist_matches.sql"

echo "✓ Done"