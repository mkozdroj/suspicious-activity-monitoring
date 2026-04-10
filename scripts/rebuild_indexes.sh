#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../../.env"

set -e

DB_NAME="${DB_NAME:-sam}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"

echo "Optimizing tables and refreshing index statistics in database: $DB_NAME"

mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" <<SQL
OPTIMIZE TABLE customer;
OPTIMIZE TABLE account;
OPTIMIZE TABLE transaction;
OPTIMIZE TABLE alert_rule;
OPTIMIZE TABLE alert;
OPTIMIZE TABLE investigation;
OPTIMIZE TABLE watchlist;
OPTIMIZE TABLE watchlist_match;

ANALYZE TABLE customer;
ANALYZE TABLE account;
ANALYZE TABLE transaction;
ANALYZE TABLE alert_rule;
ANALYZE TABLE alert;
ANALYZE TABLE investigation;
ANALYZE TABLE watchlist;
ANALYZE TABLE watchlist_match;
SQL

echo "Tables optimized and index statistics refreshed successfully."