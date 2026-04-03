-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : transaction
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

INSERT INTO transaction
    (transaction_id, transaction_ref, account_id, counterparty_account, counterparty_bank, counterparty_country, transaction_type, direction, amount, currency, amount_usd, transaction_date, value_date, status, description)
VALUES
    (1, 'TXN-2026-000001', 1, NULL, NULL, NULL, 'CASH', 'CR', 9500.0, 'GBP', 12017.5, '2026-03-01', '2026-03-01', 'COMPLETED', 'Cash deposit'),
    (2, 'TXN-2026-000002', 1, NULL, NULL, NULL, 'CASH', 'CR', 9400.0, 'GBP', 11891.8, '2026-03-02', '2026-03-02', 'COMPLETED', 'Cash deposit'),
    (3, 'TXN-2026-000003', 1, NULL, NULL, NULL, 'CASH', 'CR', 8900.0, 'GBP', 11258.3, '2026-03-03', '2026-03-03', 'COMPLETED', 'Cash deposit'),
    (4, 'TXN-2026-000004', 4, 'AE070331234567000999', 'Emirates NBD', 'AE', 'WIRE', 'DR', 480000.0, 'USD', 480000.0, '2026-03-10', '2026-03-12', 'COMPLETED', 'Business transfer'),
    (5, 'TXN-2026-000005', 4, 'IR12345678901234', 'Bank Mellat Iran', 'IR', 'WIRE', 'DR', 125000.0, 'USD', 125000.0, '2026-03-11', '2026-03-13', 'PENDING', 'Vendor payment'),
    (6, 'TXN-2026-000006', 6, 'CN12345678901', 'Bank of China', 'CN', 'WIRE', 'CR', 2100000.0, 'HKD', 269230.77, '2026-03-12', '2026-03-14', 'COMPLETED', 'Capital injection'),
    (7, 'TXN-2026-000007', 6, 'KY98765432109', 'Cayman Islands Bank', 'KY', 'WIRE', 'DR', 1950000.0, 'HKD', 250000.0, '2026-03-12', '2026-03-14', 'COMPLETED', 'Investment transfer'),
    (8, 'TXN-2026-000008', 10, 'CH5604835099988', 'UBS Geneva', 'CH', 'WIRE', 'CR', 750000.0, 'USD', 750000.0, '2026-03-13', '2026-03-15', 'COMPLETED', 'Trust income'),
    (9, 'TXN-2026-000009', 10, 'RU12345678901234', 'Sberbank Russia', 'RU', 'WIRE', 'DR', 680000.0, 'USD', 680000.0, '2026-03-13', '2026-03-15', 'COMPLETED', 'Investment'),
    (10, 'TXN-2026-000010', 3, '2034567891234', 'Monzo', 'GB', 'INTERNAL', 'DR', 95000.0, 'GBP', 120190.0, '2026-03-14', '2026-03-14', 'COMPLETED', 'Payroll run'),
    (11, 'TXN-2026-000011', 7, 'US98765432109876', 'Wells Fargo', 'US', 'WIRE', 'DR', 1200000.0, 'MXN', 59406.0, '2026-03-15', '2026-03-17', 'COMPLETED', 'Property acquisition'),
    (12, 'TXN-2026-000012', 14, 'IR99887766554433', 'Tejarat Bank', 'IR', 'WIRE', 'DR', 45000.0, 'EUR', 48825.0, '2026-03-16', '2026-03-18', 'PENDING', 'Consultancy fee'),
    (13, 'TXN-2026-000013', 11, NULL, NULL, NULL, 'CASH', 'DR', 4800.0, 'GBP', 6073.44, '2026-03-17', '2026-03-17', 'COMPLETED', 'Cash withdrawal'),
    (14, 'TXN-2026-000014', 17, 'CD12345678901234', 'Rawbank DRC', 'CD', 'WIRE', 'DR', 88000.0, 'EUR', 95546.4, '2026-03-18', '2026-03-20', 'COMPLETED', 'Family remittance'),
    (15, 'TXN-2026-000015', 20, 'CO12345678901', 'Bancolombia', 'CO', 'WIRE', 'CR', 320000.0, 'USD', 320000.0, '2026-01-08', '2026-01-10', 'COMPLETED', 'Export proceeds'),
    (16, 'TXN-2026-000016', 20, 'MX12345678901234', 'BBVA Mexico', 'MX', 'WIRE', 'DR', 315000.0, 'USD', 315000.0, '2026-01-09', '2026-01-11', 'COMPLETED', 'Import payment'),
    (17, 'TXN-2026-000017', 8, 'SG98765432109', 'DBS Singapore', 'SG', 'WIRE', 'CR', 82000.0, 'SGD', 60740.0, '2026-03-20', '2026-03-22', 'COMPLETED', 'Brokerage proceeds'),
    (18, 'TXN-2026-000018', 16, 'SY12345678901', 'Commercial Bank Syria', 'SY', 'WIRE', 'DR', 28000.0, 'CHF', 31191.2, '2026-03-21', '2026-03-23', 'PENDING', 'Humanitarian aid'),
    (19, 'TXN-2026-000019', 19, NULL, NULL, NULL, 'CASH', 'CR', 9200.0, 'GBP', 11637.96, '2026-03-22', '2026-03-22', 'COMPLETED', 'Cash deposit'),
    (20, 'TXN-2026-000020', 4, 'AE070331234599999', 'Abu Dhabi Islamic Bk', 'AE', 'WIRE', 'CR', 390000.0, 'USD', 390000.0, '2026-03-25', '2026-03-27', 'COMPLETED', 'Business revenue');
