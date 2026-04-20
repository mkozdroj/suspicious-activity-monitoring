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

MYSQL_BASE=(mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME")

echo "Clearing table 'txn' in database '$DB_NAME'..."

"${MYSQL_BASE[@]}" -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE TABLE txn; SET FOREIGN_KEY_CHECKS=1;"

echo "Table cleared. Uploading 50 transactions, one every ${SLEEP_SECONDS}s..."
echo "Press Ctrl+C to stop."

declare -a TXNS=(
  # ── Original 20 ────────────────────────────────────────────────────────────────────
  "(1,  'TXN-2026-000001', 1,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 9500.0,    'GBP', 12017.50,  '2026-03-01', '2026-03-01', 'COMPLETED', 'Cash deposit')"
  "(2,  'TXN-2026-000002', 1,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 9400.0,    'GBP', 11891.80,  '2026-03-02', '2026-03-02', 'COMPLETED', 'Cash deposit')"
  "(3,  'TXN-2026-000003', 1,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 8900.0,    'GBP', 11258.30,  '2026-03-03', '2026-03-03', 'COMPLETED', 'Cash deposit')"
  "(4,  'TXN-2026-000004', 4,  'AE070331234567000999','Emirates NBD',          'AE', 'WIRE',     'DR', 480000.0,  'USD', 480000.00, '2026-03-10', '2026-03-12', 'COMPLETED', 'Business transfer')"
  "(5,  'TXN-2026-000005', 4,  'IR12345678901234',    'Bank Mellat Iran',      'IR', 'WIRE',     'DR', 125000.0,  'USD', 125000.00, '2026-03-11', '2026-03-13', 'PENDING',   'Vendor payment')"
  "(6,  'TXN-2026-000006', 6,  'CN12345678901',       'Bank of China',         'CN', 'WIRE',     'CR', 2100000.0, 'HKD', 269230.77, '2026-03-12', '2026-03-14', 'COMPLETED', 'Capital injection')"
  "(7,  'TXN-2026-000007', 6,  'KY98765432109',       'Cayman Islands Bank',   'KY', 'WIRE',     'DR', 1950000.0, 'HKD', 250000.00, '2026-03-12', '2026-03-14', 'COMPLETED', 'Investment transfer')"
  "(8,  'TXN-2026-000008', 10, 'CH5604835099988',     'UBS Geneva',            'CH', 'WIRE',     'CR', 750000.0,  'USD', 750000.00, '2026-03-13', '2026-03-15', 'COMPLETED', 'Trust income')"
  "(9,  'TXN-2026-000009', 10, 'RU12345678901234',    'Sberbank Russia',       'RU', 'WIRE',     'DR', 680000.0,  'USD', 680000.00, '2026-03-13', '2026-03-15', 'COMPLETED', 'Investment')"
  "(10, 'TXN-2026-000010', 3,  '2034567891234',       'Monzo',                 'GB', 'INTERNAL', 'DR', 95000.0,   'GBP', 120190.00, '2026-03-14', '2026-03-14', 'COMPLETED', 'Payroll run')"
  "(11, 'TXN-2026-000011', 7,  'US98765432109876',    'Wells Fargo',           'US', 'WIRE',     'DR', 1200000.0, 'MXN', 59406.00,  '2026-03-15', '2026-03-17', 'COMPLETED', 'Property acquisition')"
  "(12, 'TXN-2026-000012', 14, 'IR99887766554433',    'Tejarat Bank',          'IR', 'WIRE',     'DR', 45000.0,   'EUR', 48825.00,  '2026-03-16', '2026-03-18', 'PENDING',   'Consultancy fee')"
  "(13, 'TXN-2026-000013', 11, NULL,                  NULL,                    NULL, 'CASH',     'DR', 4800.0,    'GBP', 6073.44,   '2026-03-17', '2026-03-17', 'COMPLETED', 'Cash withdrawal')"
  "(14, 'TXN-2026-000014', 17, 'CD12345678901234',    'Rawbank DRC',           'CD', 'WIRE',     'DR', 88000.0,   'EUR', 95546.40,  '2026-03-18', '2026-03-20', 'COMPLETED', 'Family remittance')"
  "(15, 'TXN-2026-000015', 20, 'CO12345678901',       'Bancolombia',           'CO', 'WIRE',     'CR', 320000.0,  'USD', 320000.00, '2026-01-08', '2026-01-10', 'COMPLETED', 'Export proceeds')"
  "(16, 'TXN-2026-000016', 20, 'MX12345678901234',    'BBVA Mexico',           'MX', 'WIRE',     'DR', 315000.0,  'USD', 315000.00, '2026-01-09', '2026-01-11', 'COMPLETED', 'Import payment')"
  "(17, 'TXN-2026-000017', 8,  'SG98765432109',       'DBS Singapore',         'SG', 'WIRE',     'CR', 82000.0,   'SGD', 60740.00,  '2026-03-20', '2026-03-22', 'COMPLETED', 'Brokerage proceeds')"
  "(18, 'TXN-2026-000018', 16, 'SY12345678901',       'Commercial Bank Syria', 'SY', 'WIRE',     'DR', 28000.0,   'CHF', 31191.20,  '2026-03-21', '2026-03-23', 'PENDING',   'Humanitarian aid')"
  "(19, 'TXN-2026-000019', 19, NULL,                  NULL,                    NULL, 'CASH',     'CR', 9200.0,    'GBP', 11637.96,  '2026-03-22', '2026-03-22', 'COMPLETED', 'Cash deposit')"
  "(20, 'TXN-2026-000020', 4,  'AE070331234599999',   'Abu Dhabi Islamic Bk',  'AE', 'WIRE',     'CR', 390000.0,  'USD', 390000.00, '2026-03-25', '2026-03-27', 'COMPLETED', 'Business revenue')"

  # ── STRUCTURING (STR-001/002): cash just below 10k, smurfing pattern ────────────
  "(21, 'TXN-2026-000021', 2,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 9800.0,    'GBP', 12396.60,  '2026-04-01', '2026-04-01', 'SCREENED',  'Cash deposit below CTR')"
  "(22, 'TXN-2026-000022', 2,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 9750.0,    'GBP', 12333.38,  '2026-04-02', '2026-04-02', 'SCREENED',  'Cash deposit below CTR')"
  "(23, 'TXN-2026-000023', 2,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 9600.0,    'GBP', 12143.04,  '2026-04-03', '2026-04-03', 'SCREENED',  'Cash deposit below CTR')"
  "(24, 'TXN-2026-000024', 5,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 4900.0,    'EUR', 5316.60,   '2026-04-01', '2026-04-01', 'COMPLETED', 'Small cash deposit')"
  "(25, 'TXN-2026-000025', 5,  NULL,                  NULL,                    NULL, 'CASH',     'CR', 4850.0,    'EUR', 5262.35,   '2026-04-01', '2026-04-01', 'COMPLETED', 'Small cash deposit')"

  # ── VELOCITY (VEL-001/003): rapid high-value wires, multi-currency ───────────────
  "(26, 'TXN-2026-000026', 13, 'US98765400001111',    'JPMorgan Chase',        'US', 'WIRE',     'CR', 150000.0,  'USD', 150000.00, '2026-04-02', '2026-04-04', 'COMPLETED', 'Client settlement')"
  "(27, 'TXN-2026-000027', 13, 'US98765400002222',    'Bank of America',       'US', 'WIRE',     'DR', 145000.0,  'USD', 145000.00, '2026-04-02', '2026-04-04', 'COMPLETED', 'Immediate reinvestment')"
  "(28, 'TXN-2026-000028', 13, 'GB29HSBC10001122',    'HSBC London',           'GB', 'WIRE',     'DR', 120000.0,  'USD', 120000.00, '2026-04-03', '2026-04-05', 'SCREENED',  'Rapid outbound wire')"
  "(29, 'TXN-2026-000029', 13, 'DE89370400440001',    'Deutsche Bank',         'DE', 'WIRE',     'DR', 130000.0,  'USD', 130000.00, '2026-04-03', '2026-04-05', 'SCREENED',  'Same-day international wire')"

  # ── GEOGRAPHY (GEO-001/002/003): sanctioned & high-risk jurisdictions ───────────
  "(30, 'TXN-2026-000030', 9,  'KP12345678901234',    'Korea Kwangson Bank',   'KP', 'WIRE',     'CR', 75000.0,   'USD', 75000.00,  '2026-04-04', '2026-04-06', 'BLOCKED',   'Inbound from DPRK')"
  "(31, 'TXN-2026-000031', 9,  'MM98765432109876',    'Myanmar Bank',          'MM', 'WIRE',     'DR', 42000.0,   'USD', 42000.00,  '2026-04-04', '2026-04-06', 'BLOCKED',   'Outbound to Myanmar')"
  "(32, 'TXN-2026-000032', 18, 'CU12345678901234',    'Banco de Cuba',         'CU', 'WIRE',     'CR', 18000.0,   'EUR', 19530.00,  '2026-04-05', '2026-04-07', 'PENDING',   'Remittance from Cuba')"
  "(33, 'TXN-2026-000033', 17, 'PA98765432109876',    'Balboa Bank Panama',    'PA', 'WIRE',     'DR', 220000.0,  'EUR', 238700.00, '2026-04-05', '2026-04-07', 'PENDING',   'Offshore shell transfer')"
  "(34, 'TXN-2026-000034', 15, 'VE12345678901234',    'Banco de Venezuela',    'VE', 'WIRE',     'CR', 95000.0,   'USD', 95000.00,  '2026-04-06', '2026-04-08', 'BLOCKED',   'Inbound from Venezuela')"

  # ── PATTERN (PAT-001/002/003): round numbers, round tripping, dormant acct ──────
  "(35, 'TXN-2026-000035', 12, 'IT60X0542811200001',  'UniCredit Milan',       'IT', 'WIRE',     'CR', 50000.0,   'EUR', 54250.00,  '2026-04-06', '2026-04-06', 'COMPLETED', 'Round number inbound')"
  "(36, 'TXN-2026-000036', 12, 'IT60X0542811200001',  'UniCredit Milan',       'IT', 'WIRE',     'DR', 50000.0,   'EUR', 54250.00,  '2026-04-07', '2026-04-07', 'SCREENED',  'Round trip return same amount')"
  "(37, 'TXN-2026-000037', 15, 'JP12345678900001',    'Mitsubishi UFJ',        'JP', 'WIRE',     'CR', 200000.0,  'USD', 200000.00, '2026-04-07', '2026-04-09', 'COMPLETED', 'Large round number wire')"
  "(38, 'TXN-2026-000038', 15, 'KY98765432100001',    'Cayman Intl Bank',      'KY', 'WIRE',     'DR', 200000.0,  'USD', 200000.00, '2026-04-08', '2026-04-10', 'SCREENED',  'Immediate offshore layering')"
  "(39, 'TXN-2026-000039', 18, NULL,                  NULL,                    NULL, 'CASH',     'CR', 30000.0,   'EUR', 32550.00,  '2026-04-08', '2026-04-08', 'COMPLETED', 'Dormant account reactivated')"

  # ── STRUCTURING + CRYPTO (STR-003): crypto off-ramp pattern ────────────────────
  "(40, 'TXN-2026-000040', 8,  'SG98765432100099',    'Coinbase SG',           'SG', 'CRYPTO',   'CR', 9400.0,    'SGD', 6956.00,   '2026-04-09', '2026-04-09', 'COMPLETED', 'Crypto-to-fiat conversion')"
  "(41, 'TXN-2026-000041', 8,  'SG98765432100088',    'Binance SG',            'SG', 'CRYPTO',   'CR', 9300.0,    'SGD', 6882.00,   '2026-04-09', '2026-04-09', 'COMPLETED', 'Crypto-to-fiat conversion')"
  "(42, 'TXN-2026-000042', 8,  'SG98765432100077',    'Kraken SG',             'SG', 'CRYPTO',   'CR', 9100.0,    'SGD', 6734.00,   '2026-04-10', '2026-04-10', 'SCREENED',  'Crypto-to-fiat conversion')"

  # ── VELOCITY + GEOGRAPHY: high-risk corridor (PAT-005 / GEO-004) ───────────────
  "(43, 'TXN-2026-000043', 7,  'MX12345678900001',    'BBVA Mexico',           'MX', 'WIRE',     'DR', 500000.0,  'MXN', 24750.00,  '2026-04-10', '2026-04-12', 'COMPLETED', 'Cross-border corridor wire')"
  "(44, 'TXN-2026-000044', 7,  'KY98765432109001',    'Cayman Islands Bank',   'KY', 'WIRE',     'DR', 480000.0,  'MXN', 23760.00,  '2026-04-11', '2026-04-13', 'SCREENED',  'MX to KY layering')"

  # ── CHEQUE KITING (STR-004) ─────────────────────────────────────────────────────
  "(45, 'TXN-2026-000045', 11, NULL,                  NULL,                    NULL, 'CHEQUE',   'CR', 14000.0,   'GBP', 17710.00,  '2026-04-11', '2026-04-11', 'PENDING',   'Cheque deposit')"
  "(46, 'TXN-2026-000046', 11, NULL,                  NULL,                    NULL, 'CASH',     'DR', 13500.0,   'GBP', 17078.25,  '2026-04-12', '2026-04-12', 'COMPLETED', 'Withdrawal before clearance')"

  # ── REVERSED / FAILED transactions (status diversity) ──────────────────────────
  "(47, 'TXN-2026-000047', 14, 'DE89370400440098',    'Commerzbank',           'DE', 'WIRE',     'CR', 62000.0,   'EUR', 67270.00,  '2026-04-12', '2026-04-14', 'REVERSED',  'Reversed — compliance hold')"
  "(48, 'TXN-2026-000048', 3,  'GB29BARCLAYS123456',  'Barclays',              'GB', 'INTERNAL', 'DR', 50000.0,   'GBP', 63250.00,  '2026-04-13', '2026-04-13', 'FAILED',    'Failed — insufficient funds')"

  # ── WATCHLIST / PEP high-value (WL-002) ─────────────────────────────────────────
  "(49, 'TXN-2026-000049', 16, 'CH5604835012300001',  'Credit Suisse',         'CH', 'WIRE',     'CR', 95000.0,   'CHF', 105830.00, '2026-04-13', '2026-04-15', 'SCREENED',  'PEP inbound high value')"
  "(50, 'TXN-2026-000050', 16, 'AE070331234512345',   'Abu Dhabi Comm Bank',   'AE', 'WIRE',     'DR', 90000.0,   'CHF', 100260.00, '2026-04-14', '2026-04-16', 'PENDING',   'PEP offshore transfer')"
)

total=${#TXNS[@]}

for i in "${!TXNS[@]}"; do
  row="${TXNS[$i]}"
  num=$((i + 1))

  "${MYSQL_BASE[@]}" -e "
    INSERT INTO txn
      (txn_id, txn_ref, account_id, counterparty_account, counterparty_bank, counterparty_country,
       txn_type, direction, amount, currency, amount_usd, txn_date, value_date, status, description)
    VALUES ${row};
  "

  txn_ref=$(echo "$row" | grep -oE "'TXN-[^']*'" | head -1 | tr -d "'")
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ($num/$total) Inserted: $txn_ref"

  if [ "$num" -lt "$total" ]; then
    sleep "$SLEEP_SECONDS"
  fi
done

echo "✓ All $total transactions uploaded."

