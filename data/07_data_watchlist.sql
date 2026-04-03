-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : watchlist
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

INSERT INTO watchlist
    (watchlist_id, list_type, entity_name, entity_type, country, date_of_birth, reason, listed_date, is_active)
VALUES
    (1, 'OFAC', 'Narco Shell Corp', 'ENTITY', 'PA', NULL, 'SDN — narcotics trafficking financing', '2021-06-15', TRUE),
    (2, 'OFAC', 'Bank Mellat', 'ENTITY', 'IR', NULL, 'SDN — Iranian state bank, sanctions evasion', '2012-01-23', TRUE),
    (3, 'UN', 'Tejarat Bank', 'ENTITY', 'IR', NULL, 'UN Security Council sanctions — proliferation financing', '2010-09-09', TRUE),
    (4, 'HMT', 'Commercial Bank of Syria', 'ENTITY', 'SY', NULL, 'HMT financial sanctions — Syria conflict financing', '2012-03-01', TRUE),
    (5, 'EU', 'Rossiya Bank', 'ENTITY', 'RU', NULL, 'EU sanctions — linked to destabilisation of Ukraine', '2022-03-01', TRUE),
    (6, 'OFAC', 'Viktor Alexandr Sokolov', 'INDIVIDUAL', 'RU', '1963-11-03', 'SDN — oligarch linked to sanctioned Russian entities', '2022-04-06', TRUE),
    (7, 'INTERPOL', 'Juan Carlos Esparza Rios', 'INDIVIDUAL', 'CO', '1971-04-14', 'Red notice — money laundering and narcotics', '2019-11-20', TRUE),
    (8, 'INTERNAL', 'Greenleaf Trust Company', 'ENTITY', 'KY', NULL, 'Internal watch — beneficial ownership unresolved', '2024-02-01', TRUE),
    (9, 'OFAC', 'Mahan Air', 'ENTITY', 'IR', NULL, 'SDN — IRGC-linked airline', '2011-12-09', TRUE),
    (10, 'UN', 'Kim Chol', 'INDIVIDUAL', 'KP', '1968-09-18', 'UN DPRK sanctions — WMD financing', '2016-11-30', TRUE),
    (11, 'EU', 'Belarus Potash Company', 'ENTITY', 'BY', NULL, 'EU sanctions — Lukashenko regime financing', '2021-08-09', TRUE),
    (12, 'HMT', 'Abdullahi Al-Shabaab Network', 'ENTITY', 'SO', NULL, 'HMT — terrorist financing (Al-Shabaab)', '2010-03-12', TRUE),
    (13, 'OFAC', 'Tethys Petroleum', 'ENTITY', 'RU', NULL, 'SDN — Russian energy sector restricted entity', '2022-06-28', TRUE),
    (14, 'INTERNAL', 'Pacific Bridge Holdings', 'ENTITY', 'HK', NULL, 'Internal watch — unresolved UBO, layering indicators', '2024-03-01', TRUE),
    (15, 'PEP', 'Carlos Mendez Ruiz', 'INDIVIDUAL', 'MX', '1955-03-28', 'Former Mexican Transport Minister — Class B PEP', '2017-05-14', TRUE),
    (16, 'PEP', 'Andre Mobutu', 'INDIVIDUAL', 'CD', '1968-09-01', 'Current DRC State Mining Authority Director — Class A PEP', '2020-03-12', TRUE),
    (17, 'OFAC', 'Crypto Exchange Bitzlato', 'ENTITY', 'HK', NULL, 'SDN — primary money laundering for ransomware actors', '2023-01-18', TRUE),
    (18, 'EU', 'Promsvyazbank', 'ENTITY', 'RU', NULL, 'EU/UK sanctions — Russian defence bank', '2022-02-28', TRUE),
    (19, 'INTERPOL', 'Huang Wei (alias: Michael Wan)', 'INDIVIDUAL', 'CN', '1975-06-06', 'Red notice — trade-based money laundering', '2021-09-14', TRUE),
    (20, 'HMT', 'Khurram Shah', 'INDIVIDUAL', 'PK', '1980-12-11', 'HMT — financing terrorism (Lashkar-e-Taiba links)', '2020-07-07', TRUE);
