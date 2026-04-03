#!/bin/bash
source ../.env
set -e

DB_NAME="${DB_NAME:-sam_db}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-root}"
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
OUTPUT_FILE="${1:-open_cases_report.csv}"

echo "Exporting open cases report to: $OUTPUT_FILE"

mysql \
  -h"$DB_HOST" \
  -P"$DB_PORT" \
  -u"$DB_USER" \
  -p"$DB_PASSWORD" \
  --batch --raw --silent \
  -e "
SELECT 'alert_ref','alert_status','triggered_at','rule_code','rule_name','account_number','customer_ref','full_name','risk_rating','investigation_ref','priority','opened_by','opened_at'
UNION ALL
SELECT
  a.alert_ref,
  a.status,
  a.triggered_at,
  ar.rule_code,
  ar.rule_name,
  acc.account_number,
  c.customer_ref,
  c.full_name,
  c.risk_rating,
  COALESCE(i.investigation_ref, ''),
  COALESCE(i.priority, ''),
  COALESCE(i.opened_by, ''),
  COALESCE(i.opened_at, '')
FROM alert a
JOIN alert_rule ar ON a.rule_id = ar.rule_id
JOIN account acc ON a.account_id = acc.account_id
JOIN customer c ON acc.customer_id = c.customer_id
LEFT JOIN investigation i ON i.alert_id = a.alert_id
WHERE a.status IN ('OPEN', 'UNDER_REVIEW', 'ESCALATED')
" "$DB_NAME" | sed 's/\t/,/g' > "$OUTPUT_FILE"

echo "Report exported successfully."