-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : watchlist_match
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

INSERT INTO watchlist_match
    (match_id, txn_id, watchlist_id, match_type, match_score, matched_field, matched_value, status, reviewed_by, reviewed_at)
VALUES
    (1, 15, 1, 'NAME', 100.0, 'customer_name', 'Narco Shell Corp', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-01-08 08:05:00'),
    (2, 16, 1, 'NAME', 100.0, 'customer_name', 'Narco Shell Corp', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-01-08 08:05:00'),
    (3, 5, 2, 'NAME', 98.5, 'counterparty_bank', 'Bank Mellat Iran', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-03-11 15:30:00'),
    (4, 12, 3, 'NAME', 97.0, 'counterparty_bank', 'Tejarat Bank', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-16 12:30:00'),
    (5, 18, 4, 'NAME', 100.0, 'counterparty_bank', 'Commercial Bank Syria', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-03-21 16:05:00'),
    (6, 9, 5, 'COUNTRY', 85.0, 'counterparty_country', 'RU', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-13 14:00:00'),
    (7, 4, 6, 'FUZZY_NAME', 91.0, 'customer_name', 'Viktor Sokolov', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-03-12 09:00:00'),
    (8, 11, 15, 'NAME', 100.0, 'customer_name', 'Carlos Mendez Ruiz', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-15 13:30:00'),
    (9, 14, 16, 'NAME', 100.0, 'customer_name', 'Andre Mobutu', 'CONFIRMED', 'aml.analyst1@hsbc.com', '2026-03-18 17:15:00'),
    (10, 6, 14, 'NAME', 95.0, 'customer_name', 'Pacific Bridge Holdings', 'CONFIRMED', 'aml.analyst1@hsbc.com', '2026-03-12 17:30:00'),
    (11, 8, 8, 'NAME', 95.0, 'customer_name', 'Greenleaf Trust Company', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-13 10:45:00'),
    (12, 9, 18, 'NAME', 88.0, 'counterparty_bank', 'Sberbank Russia', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-13 15:00:00'),
    (13, 20, 6, 'FUZZY_NAME', 89.0, 'customer_name', 'Viktor Sokolov', 'CONFIRMED', 'aml.analyst2@hsbc.com', '2026-03-25 11:00:00'),
    (14, 1, 11, 'FUZZY_NAME', 42.0, 'customer_name', 'James Thorton', 'FALSE_POSITIVE', 'aml.analyst1@hsbc.com', '2026-03-05 10:00:00'),
    (15, 10, 7, 'NAME', 35.0, 'customer_name', 'Meridian Trad Ltd', 'FALSE_POSITIVE', 'aml.analyst1@hsbc.com', '2026-03-15 11:00:00'),
    (16, 17, 19, 'FUZZY_NAME', 72.0, 'counterparty_name', 'Liu Wei', 'PENDING', NULL, NULL),
    (17, 3, 9, 'FUZZY_NAME', 38.0, 'customer_name', 'James Thornton', 'FALSE_POSITIVE', 'aml.analyst1@hsbc.com', '2026-03-10 09:00:00'),
    (18, 7, 8, 'ACCOUNT', 78.0, 'counterparty_account', 'KY98765432109', 'CONFIRMED', 'aml.analyst1@hsbc.com', '2026-03-13 09:00:00'),
    (19, 12, 20, 'FUZZY_NAME', 65.0, 'counterparty_country', 'IR', 'PENDING', NULL, NULL),
    (20, 18, 4, 'COUNTRY', 90.0, 'counterparty_country', 'SY', 'CONFIRMED', 'aml.senior@hsbc.com', '2026-03-21 16:10:00');
