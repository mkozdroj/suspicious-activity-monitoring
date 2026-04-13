# Project 04 — Suspicious Activity Monitor: Sample Data

## Load order

Run `00_ddl_schema.sql` first to create all tables, then load data files in numbered order:

```
01_data_customers.sql
02_data_accounts.sql
03_data_transactions.sql
04_data_alert_rules.sql
05_data_alerts.sql
06_data_investigations.sql
07_data_watchlist.sql
08_data_watchlist_matches.sql
```

## Tables (20 rows each)

| File | Table | Description |
|------|-------|-------------|
| 01 | `customer` | 20 customers: individuals, corporates, trusts, charities across multiple jurisdictions and risk ratings (LOW → SANCTIONED) |
| 02 | `account` | 20 accounts: GBP/EUR/USD/HKD/SGD; statuses include ACTIVE, FROZEN, RESTRICTED |
| 03 | `txn` | 20 transactions: WIRE, CASH, INTERNAL; includes Iran/Syria/Russia/Cayman counterparties and structuring patterns |
| 04 | `alert_rule` | 20 AML detection rules: structuring, velocity, geography, watchlist, pattern-based |
| 05 | `alert` | 20 alerts: OPEN → UNDER_REVIEW → ESCALATED → SAR_FILED; two confirmed SARs included |
| 06 | `investigation` | 20 investigations: detailed findings, outcomes (SAR_FILED, NO_ACTION, ACCOUNT_CLOSED) |
| 07 | `watchlist` | 20 watchlist entries: OFAC, UN, HMT, EU, INTERPOL, INTERNAL, PEP lists |
| 08 | `watchlist_match` | 20 matches: NAME, FUZZY_NAME, ACCOUNT, COUNTRY; includes false positives and confirmed hits |

## Compatibility

| Feature | PostgreSQL | MySQL 8+ | Oracle | SQL Server |
|---------|-----------|----------|--------|------------|
| BOOLEAN | ✓ | ✓ | Use NUMBER(1) | Use BIT |
| TIMESTAMP | ✓ | ✓ | Use TO_TIMESTAMP | ✓ |
| SMALLINT | ✓ | ✓ | ✓ | ✓ |
| DECIMAL | ✓ | ✓ | Use NUMBER | ✓ |

## Key domain concepts

- **SAR (Suspicious Activity Report)**: Regulatory report filed with the Financial Intelligence Unit (NCA in UK, FinCEN in US) when money laundering or terrorist financing is suspected
- **CTR (Currency Transaction Report)**: Mandatory report for cash transactions above $10,000 (US) / £10,000 (UK)
- **Structuring / Smurfing**: Breaking large cash transactions into smaller amounts to avoid CTR reporting thresholds — a federal crime
- **PEP (Politically Exposed Person)**: Senior government officials and their families — subject to Enhanced Due Diligence (EDD)
- **OFAC SDN List**: US Treasury list of Specially Designated Nationals — transactions with listed entities are prohibited
- **HMT / OFSI**: UK equivalent of OFAC — His Majesty's Treasury / Office of Financial Sanctions Implementation
- **KYC Expired**: Customer whose identity verification is out of date — triggers re-verification before further transactions
- **Round-tripping / Layering**: Funds moved through multiple accounts/jurisdictions to obscure origin — classic money laundering technique

## Useful queries

```sql
-- 1. Open high-severity alerts with customer details
SELECT al.alert_ref, ar.rule_name, ar.severity, c.full_name, c.risk_rating,
       al.alert_score, al.triggered_at, al.status
FROM alert al
JOIN alert_rule ar ON al.rule_id = ar.rule_id
JOIN account ac   ON al.account_id = ac.account_id
JOIN customer c   ON ac.customer_id = c.customer_id
WHERE al.status IN ('OPEN','ESCALATED')
  AND ar.severity IN ('HIGH','CRITICAL')
ORDER BY al.alert_score DESC;

-- 2. Transactions to/from high-risk countries in last 30 days
SELECT t.transaction_ref, c.full_name, t.counterparty_country,
       t.transaction_type, t.direction, t.amount_usd, t.transaction_date
FROM txn t
JOIN account a   ON t.account_id = a.account_id
JOIN customer c  ON a.customer_id = c.customer_id
WHERE t.counterparty_country IN ('IR','SY','KP','RU','MM','KW','BY','CU')
  AND t.transaction_date >= CURRENT_DATE - INTERVAL '30 days'
ORDER BY t.amount_usd DESC;

-- 3. Watchlist matches awaiting review
SELECT wm.match_id, wm.match_type, wm.match_score, wl.entity_name,
       wl.list_type, wm.matched_field, wm.matched_value, t.transaction_ref
FROM watchlist_match wm
JOIN watchlist wl        ON wm.watchlist_id = wl.watchlist_id
JOIN txn t       ON wm.transaction_id = t.transaction_id
WHERE wm.status = 'PENDING'
ORDER BY wm.match_score DESC;
```
