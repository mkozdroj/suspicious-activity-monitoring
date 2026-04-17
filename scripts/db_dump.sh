#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/../.env"

set -e

DB_NAME="${DB_NAME:-sam}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
TIMESTAMP="$(date +'%Y-%m-%d_%H-%M-%S')"
DUMP_FILE="${1:-sam_dump_$TIMESTAMP.sql}"

echo "Creating MySQL dump: $DUMP_FILE"

mysqldump \
  -h"$DB_HOST" \
  -P"$DB_PORT" \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  --routines \
  --triggers \
  --single-transaction \
  --add-drop-table \
  "$DB_NAME" > "$DUMP_FILE"

echo "Dump created successfully: $DUMP_FILE"