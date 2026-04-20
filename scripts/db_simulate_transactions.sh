#!/bin/bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ -f "$SCRIPT_DIR/../.env" ]; then
  source "$SCRIPT_DIR/../.env"
fi

DB_NAME="${DB_NAME:-sam}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
SLEEP_SECONDS="${SLEEP_SECONDS:-5}"

EXTRA_TXN_SQL="$SCRIPT_DIR/../data/09_simulate_transactions_data.sql"

MYSQL_BASE=(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME")

if [ ! -f "$EXTRA_TXN_SQL" ]; then
  echo "Error: extra transaction SQL not found: $EXTRA_TXN_SQL"
  exit 1
fi

total=$(grep -cve '^[[:space:]]*$' -e '^[[:space:]]*--' "$EXTRA_TXN_SQL")

if [ "$total" -eq 0 ]; then
  echo "Error: no transaction statements found in $EXTRA_TXN_SQL"
  exit 1
fi

echo "Simulating ${total} additional transactions, one every ${SLEEP_SECONDS}s..."
echo "Press Ctrl+C to stop."

num=0

while IFS= read -r sql_line || [ -n "$sql_line" ]; do
  if printf '%s\n' "$sql_line" | grep -Eq '^[[:space:]]*$|^[[:space:]]*--'; then
    continue
  fi

  num=$((num + 1))
  "${MYSQL_BASE[@]}" -e "$sql_line"

  txn_ref=$(printf '%s\n' "$sql_line" | sed -E "s/.*'([^']+)'.*/\1/")
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ($num/$total) Loaded: $txn_ref"

  if [ "$num" -lt "$total" ]; then
    sleep "$SLEEP_SECONDS"
  fi
done < "$EXTRA_TXN_SQL"

echo "✓ Simulated $total additional transactions successfully."
