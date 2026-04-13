-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : alert
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

INSERT INTO alert
    (alert_id, alert_ref, rule_id, account_id, txn_id, triggered_at, alert_score, status, assigned_to, notes)
VALUES
    (1, 'ALT-2026-0001', 1, 1, 3, '2026-03-03 09:15:00', 78, 'UNDER_REVIEW', 'aml.analyst1@hsbc.com', 'Three consecutive cash deposits just below £10K over 3 days'),
    (2, 'ALT-2026-0002', 5, 4, 5, '2026-03-11 14:22:00', 92, 'ESCALATED', 'aml.senior@hsbc.com', 'Wire to Iranian bank — sanctions review required'),
    (3, 'ALT-2026-0003', 5, 14, 12, '2026-03-16 11:05:00', 88, 'UNDER_REVIEW', 'aml.analyst2@hsbc.com', 'Wire to Iranian bank Tejarat — OFAC check pending'),
    (4, 'ALT-2026-0004', 9, 6, 7, '2026-03-12 16:40:00', 85, 'UNDER_REVIEW', 'aml.analyst1@hsbc.com', 'Funds in from China, out to Cayman same day'),
    (5, 'ALT-2026-0005', 14, 10, 8, '2026-03-13 10:30:00', 71, 'OPEN', NULL, 'Trust account transfer via Cayman Islands bank'),
    (6, 'ALT-2026-0006', 3, 4, 20, '2026-03-25 09:00:00', 65, 'OPEN', NULL, 'Third large UAE wire in 15 days — velocity check'),
    (7, 'ALT-2026-0007', 8, 7, 11, '2026-03-15 13:10:00', 74, 'UNDER_REVIEW', 'aml.analyst2@hsbc.com', 'PEP (former minister) sending >$50K offshore'),
    (8, 'ALT-2026-0008', 6, 20, 15, '2026-01-08 08:00:00', 99, 'SAR_FILED', 'aml.senior@hsbc.com', 'Sanctioned entity — SAR filed, account frozen'),
    (9, 'ALT-2026-0009', 5, 16, 18, '2026-03-21 15:55:00', 95, 'ESCALATED', 'aml.senior@hsbc.com', 'Transfer to Syrian bank — possible sanctions breach'),
    (10, 'ALT-2026-0010', 10, 6, 6, '2026-03-12 17:00:00', 80, 'UNDER_REVIEW', 'aml.analyst1@hsbc.com', 'Layering suspected: HK acct receives and immediately retransfers'),
    (11, 'ALT-2026-0011', 1, 19, 19, '2026-03-22 12:00:00', 58, 'OPEN', NULL, 'Cash deposit £9,200 — single event below threshold, flagged for pattern'),
    (12, 'ALT-2026-0012', 14, 7, 11, '2026-03-15 14:00:00', 62, 'CLOSED', 'aml.analyst1@hsbc.com', 'MXN transfer to US — reviewed, confirmed legitimate property purchase'),
    (13, 'ALT-2026-0013', 11, 3, NULL, '2026-03-20 09:00:00', 55, 'OPEN', NULL, 'Corporate account 6-month dormancy broken with large credit'),
    (14, 'ALT-2026-0014', 7, 20, 15, '2026-01-08 08:01:00', 100, 'SAR_FILED', 'aml.senior@hsbc.com', 'OFAC watchlist match confirmed — Narco Shell Corp'),
    (15, 'ALT-2026-0015', 4, 10, 9, '2026-03-13 11:00:00', 82, 'UNDER_REVIEW', 'aml.analyst2@hsbc.com', 'Frozen trust account: $750K in / $680K out same day to Russia'),
    (16, 'ALT-2026-0016', 8, 17, 14, '2026-03-18 16:30:00', 70, 'UNDER_REVIEW', 'aml.analyst1@hsbc.com', 'PEP wire to DRC — enhanced due diligence required'),
    (17, 'ALT-2026-0017', 2, 1, 1, '2026-03-01 18:00:00', 60, 'CLOSED', 'aml.analyst1@hsbc.com', 'Reviewed — salary deposits confirmed by employer letter'),
    (18, 'ALT-2026-0018', 20, 16, 18, '2026-03-21 16:00:00', 68, 'OPEN', NULL, 'Charity sending $28K to Syria — humanitarian claim unverified'),
    (19, 'ALT-2026-0019', 19, 10, 9, '2026-03-13 12:00:00', 77, 'UNDER_REVIEW', 'aml.analyst2@hsbc.com', 'Cayman trust→Russia wire corridor alert'),
    (20, 'ALT-2026-0020', 5, 9, 9, '2026-03-13 10:45:00', 91, 'ESCALATED', 'aml.senior@hsbc.com', 'Wire to Russia from trust account under freeze review');
