-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : alert_rule
--  Rows  : 20
--
--  Compatible with: PostgreSQL, MySQL 8+
--  Oracle note    : Replace BOOLEAN with NUMBER(1),
--                   TIMESTAMP literals with TO_TIMESTAMP
--
--  Load order:
--    01_customers → 02_accounts → 03_transactions →
--    04_alert_rules → 05_alerts → 06_investigations →
--    07_watchlist → 08_watchlist_matches
-- ============================================================

INSERT INTO alert_rule
    (rule_id, rule_code, rule_name, rule_category, description, threshold_amount, threshold_count, lookback_days, severity, is_active)
VALUES
    (1, 'STR-001', 'Cash Structuring Below Reporting Threshold', 'STRUCTURING', 'Multiple cash transactions just below £10,000 CTR threshold within 5 days', 10000.0, 3, 5, 'HIGH', TRUE),
    (2, 'STR-002', 'Smurfing Pattern Detection', 'STRUCTURING', 'Multiple small cash deposits across linked accounts in 24 hours', 5000.0, 5, 1, 'HIGH', TRUE),
    (3, 'VEL-001', 'High-Value Wire Velocity', 'VELOCITY', 'More than 3 international wires exceeding $100K in 7 days', 100000.0, 3, 7, 'CRITICAL', TRUE),
    (4, 'VEL-002', 'Rapid Account Turnover', 'VELOCITY', 'Account balance drained by 90%+ within 48 hours of large credit', NULL, NULL, 2, 'HIGH', TRUE),
    (5, 'GEO-001', 'High-Risk Country Wire', 'GEOGRAPHY', 'Wire transfer to/from FATF high-risk jurisdiction (IR, KP, SY, MM, etc.)', NULL, NULL, 90, 'CRITICAL', TRUE),
    (6, 'GEO-002', 'Sanctions Country Transaction', 'GEOGRAPHY', 'Any transaction involving OFAC/HMT sanctioned country', NULL, NULL, 90, 'CRITICAL', TRUE),
    (7, 'WL-001', 'Watchlist Name Match', 'WATCHLIST', 'Customer or counterparty name matches OFAC / UN / HMT watchlist', NULL, NULL, NULL, 'CRITICAL', TRUE),
    (8, 'WL-002', 'PEP High-Value Transfer', 'WATCHLIST', 'Politically Exposed Person transfers >$50K in single transaction', 50000.0, NULL, NULL, 'HIGH', TRUE),
    (9, 'PAT-001', 'Round Tripping', 'PATTERN', 'Funds leave account and return within 3 days via different route', NULL, NULL, 3, 'HIGH', TRUE),
    (10, 'PAT-002', 'Layering via Multiple Accounts', 'PATTERN', 'Funds pass through 3+ linked accounts within 24 hours', NULL, NULL, 1, 'HIGH', TRUE),
    (11, 'PAT-003', 'Dormant Account Suddenly Active', 'PATTERN', 'Account inactive 180+ days receives large credit then outward transfer', 25000.0, NULL, 180, 'MEDIUM', TRUE),
    (12, 'STR-003', 'Crypto Off-Ramp Pattern', 'STRUCTURING', 'Multiple crypto-to-fiat conversions below reporting threshold', 9500.0, 3, 7, 'HIGH', TRUE),
    (13, 'VEL-003', 'Unusual Foreign Exchange Activity', 'VELOCITY', 'Customer converts >5 different currencies in rolling 30-day period', NULL, 5, 30, 'MEDIUM', TRUE),
    (14, 'GEO-003', 'Offshore Shell Jurisdiction', 'GEOGRAPHY', 'Counterparty bank in known shell company jurisdiction (KY, BVI, PA)', NULL, NULL, 90, 'MEDIUM', TRUE),
    (15, 'WL-003', 'Adverse Media Match', 'WATCHLIST', 'Customer name matches adverse media screening result', NULL, NULL, NULL, 'MEDIUM', TRUE),
    (16, 'PAT-004', 'Trade-Based Money Laundering', 'PATTERN', 'Significant mismatch between declared trade volumes and transaction values', NULL, NULL, 90, 'HIGH', TRUE),
    (17, 'STR-004', 'Cheque Kiting', 'STRUCTURING', 'Cheque deposits followed by rapid withdrawal before clearance', NULL, NULL, 5, 'MEDIUM', TRUE),
    (18, 'VEL-004', 'Account Balance Concentration', 'VELOCITY', 'Account holds balance more than 10x prior 12-month average', NULL, NULL, 365, 'MEDIUM', TRUE),
    (19, 'GEO-004', 'High-Risk Corridor Wire', 'GEOGRAPHY', 'Wire in predefined high-risk money-laundering corridor (e.g. MX→US→KY)', NULL, NULL, 30, 'HIGH', TRUE),
    (20, 'PAT-005', 'Charity / NGO Fund Diversion', 'PATTERN', 'Charitable org receiving large donations quickly transferred offshore', 100000.0, NULL, 30, 'HIGH', TRUE);
