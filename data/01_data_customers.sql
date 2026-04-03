-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : customer
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

INSERT INTO customer
    (customer_id, customer_ref, full_name, date_of_birth, nationality, country_of_residence, customer_type, risk_rating, kyc_status, onboarded_date, is_pep, is_active)
VALUES
    (1, 'CUST-00001', 'James Thornton', '1978-04-12', 'GB', 'GB', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2018-03-01', FALSE, TRUE),
    (2, 'CUST-00002', 'Sophia Andersson', '1985-09-22', 'SE', 'GB', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2019-07-15', FALSE, TRUE),
    (3, 'CUST-00003', 'Meridian Trading Ltd', NULL, 'GB', 'GB', 'CORPORATE', 'MEDIUM', 'VERIFIED', '2020-01-10', FALSE, TRUE),
    (4, 'CUST-00004', 'Viktor Sokolov', '1963-11-03', 'RU', 'AE', 'INDIVIDUAL', 'HIGH', 'VERIFIED', '2021-02-28', FALSE, TRUE),
    (5, 'CUST-00005', 'Amara Diallo', '1990-06-17', 'SN', 'FR', 'INDIVIDUAL', 'MEDIUM', 'VERIFIED', '2020-09-05', FALSE, TRUE),
    (6, 'CUST-00006', 'Pacific Bridge Holdings', NULL, 'HK', 'HK', 'CORPORATE', 'HIGH', 'PENDING', '2023-11-20', FALSE, TRUE),
    (7, 'CUST-00007', 'Carlos Mendez Ruiz', '1955-03-28', 'MX', 'MX', 'INDIVIDUAL', 'PEP', 'VERIFIED', '2017-05-14', TRUE, TRUE),
    (8, 'CUST-00008', 'Liu Wei', '1982-08-09', 'CN', 'SG', 'INDIVIDUAL', 'MEDIUM', 'VERIFIED', '2022-04-03', FALSE, TRUE),
    (9, 'CUST-00009', 'Fatima Al-Rashidi', '1974-12-30', 'KW', 'GB', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2016-08-22', FALSE, TRUE),
    (10, 'CUST-00010', 'Greenleaf Trust Company', NULL, 'KY', 'KY', 'TRUST', 'HIGH', 'EXPIRED', '2019-03-07', FALSE, TRUE),
    (11, 'CUST-00011', 'Benjamin Osei', '1988-07-14', 'GH', 'GB', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2021-10-18', FALSE, TRUE),
    (12, 'CUST-00012', 'Elena Marchetti', '1971-05-25', 'IT', 'IT', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2015-12-01', FALSE, TRUE),
    (13, 'CUST-00013', 'Dusk Capital Partners LLC', NULL, 'US', 'US', 'CORPORATE', 'MEDIUM', 'VERIFIED', '2020-06-30', FALSE, TRUE),
    (14, 'CUST-00014', 'Hamid Rahimi', '1960-02-14', 'IR', 'DE', 'INDIVIDUAL', 'HIGH', 'VERIFIED', '2022-08-11', FALSE, TRUE),
    (15, 'CUST-00015', 'Yuki Tanaka', '1995-10-08', 'JP', 'JP', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2023-01-25', FALSE, TRUE),
    (16, 'CUST-00016', 'Atlas Charitable Foundation', NULL, 'CH', 'CH', 'CHARITY', 'MEDIUM', 'VERIFIED', '2018-07-19', FALSE, TRUE),
    (17, 'CUST-00017', 'Andre Mobutu', '1968-09-01', 'CD', 'BE', 'INDIVIDUAL', 'PEP', 'VERIFIED', '2020-03-12', TRUE, TRUE),
    (18, 'CUST-00018', 'Niamh O''Brien', '1992-04-16', 'IE', 'IE', 'INDIVIDUAL', 'LOW', 'VERIFIED', '2021-05-07', FALSE, TRUE),
    (19, 'CUST-00019', 'Rashid Al-Farouq', '1979-01-22', 'SA', 'GB', 'INDIVIDUAL', 'MEDIUM', 'VERIFIED', '2019-11-30', FALSE, TRUE),
    (20, 'CUST-00020', 'Narco Shell Corp', NULL, 'PA', 'PA', 'CORPORATE', 'SANCTIONED', 'BLOCKED', '2024-01-05', FALSE, FALSE);
