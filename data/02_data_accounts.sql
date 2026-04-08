-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : account
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

INSERT INTO account
    (account_id, account_number, customer_id, account_type, currency, balance, opened_date, status, branch_code)
VALUES
    (1, 'GB29HSBC10204010000001', 1, 'CURRENT', 'GBP', 52400.0, '2018-03-01', 'ACTIVE', 'LON001'),
    (2, 'GB29HSBC10204010000002', 2, 'SAVINGS', 'GBP', 18750.0, '2019-07-15', 'ACTIVE', 'LON001'),
    (3, 'GB29HSBC10204010000003', 3, 'CURRENT', 'GBP', 284000.0, '2020-01-10', 'ACTIVE', 'LON002'),
    (4, 'AE070331234567890123456', 4, 'CURRENT', 'USD', 1250000.0, '2021-02-28', 'ACTIVE', 'DXB001'),
    (5, 'FR7630004000031234567890', 5, 'SAVINGS', 'EUR', 9800.0, '2020-09-05', 'ACTIVE', 'PAR001'),
    (6, 'HK12345678901234567890', 6, 'CURRENT', 'HKD', 4200000.0, '2023-11-20', 'RESTRICTED', 'HKG001'),
    (7, 'MX123456789012345678', 7, 'CURRENT', 'MXN', 3800000.0, '2017-05-14', 'ACTIVE', 'MEX001'),
    (8, 'SG12345678901234567', 8, 'TRADING', 'SGD', 175000.0, '2022-04-03', 'ACTIVE', 'SIN001'),
    (9, 'GB29HSBC10204010000009', 9, 'CURRENT', 'GBP', 31200.0, '2016-08-22', 'ACTIVE', 'LON003'),
    (10, 'KY12345678901234567', 10, 'SAVINGS', 'USD', 8750000.0, '2019-03-07', 'FROZEN', 'CYM001'),
    (11, 'GB29HSBC10204010000011', 11, 'CURRENT', 'GBP', 4200.0, '2021-10-18', 'ACTIVE', 'LON001'),
    (12, 'IT60X0542811101000000123456', 12, 'SAVINGS', 'EUR', 22300.0, '2015-12-01', 'ACTIVE', 'MIL001'),
    (13, 'US12345678901234567890', 13, 'CURRENT', 'USD', 920000.0, '2020-06-30', 'ACTIVE', 'NYC001'),
    (14, 'DE89370400440532013000', 14, 'CURRENT', 'EUR', 67400.0, '2022-08-11', 'ACTIVE', 'FRA001'),
    (15, 'JP12345678901234567890', 15, 'SAVINGS', 'JPY', 1820000, '2023-01-25', 'ACTIVE', 'TKY001'),
    (16, 'CH5604835012345678009', 16, 'CURRENT', 'CHF', 385000.0, '2018-07-19', 'ACTIVE', 'ZRH001'),
    (17, 'BE68539007547034', 17, 'CURRENT', 'EUR', 115000.0, '2020-03-12', 'ACTIVE', 'BRU001'),
    (18, 'IE29AIBK93115212345678', 18, 'CURRENT', 'EUR', 7800.0, '2021-05-07', 'ACTIVE', 'DUB001'),
    (19, 'GB29HSBC10204010000019', 19, 'CURRENT', 'GBP', 41600.0, '2019-11-30', 'ACTIVE', 'LON002'),
    (20, 'PA12345678901234567890', 20, 'CURRENT', 'USD', 0.0, '2024-01-05', 'FROZEN', 'PAN001');
