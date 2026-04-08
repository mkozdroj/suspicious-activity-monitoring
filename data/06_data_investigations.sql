-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor — Sample Data
--  Table : investigation
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

INSERT INTO investigation
    (investigation_id, investigation_ref, alert_id, customer_id, opened_by, opened_at, closed_at, outcome, priority, findings)
VALUES
    (1, 'INV-2026-001', 8, 20, 'aml.senior@hsbc.com', '2026-01-08 09:00:00', '2026-01-15 17:00:00', 'SAR_FILED', 'URGENT', 'Narco Shell Corp matched OFAC SDN list. SAR filed with NCA. Account frozen per policy.'),
    (2, 'INV-2026-002', 14, 20, 'aml.senior@hsbc.com', '2026-01-08 09:01:00', '2026-01-15 17:01:00', 'ACCOUNT_CLOSED', 'URGENT', 'Watchlist match confirmed. Account closed and reported to regulators.'),
    (3, 'INV-2026-003', 2, 4, 'aml.senior@hsbc.com', '2026-03-11 15:00:00', NULL, NULL, 'URGENT', 'Customer wired $125K to Bank Mellat Iran — sanctioned entity. OFAC check initiated. Customer account restricted pending review.'),
    (4, 'INV-2026-004', 4, 6, 'aml.analyst1@hsbc.com', '2026-03-12 17:30:00', NULL, NULL, 'HIGH', 'HKD 2.1M received from China, HKD 1.95M immediately sent to Cayman Islands. Round-trip layering pattern under investigation.'),
    (5, 'INV-2026-005', 15, 10, 'aml.analyst2@hsbc.com', '2026-03-13 12:00:00', NULL, NULL, 'HIGH', 'Cayman-registered trust received $750K then transferred $680K to Russian bank same day. Trust KYC expired.'),
    (6, 'INV-2026-006', 9, 16, 'aml.senior@hsbc.com', '2026-03-21 16:30:00', NULL, NULL, 'URGENT', 'Wire transfer to Commercial Bank of Syria by Swiss charity. Syria sanctioned. Humanitarian exemption process initiated.'),
    (7, 'INV-2026-007', 7, 7, 'aml.analyst2@hsbc.com', '2026-03-15 14:00:00', NULL, NULL, 'HIGH', 'Former Mexican Transport Minister (PEP) transferred $59K USD equivalent to US. EDD requested. SOF documentation outstanding.'),
    (8, 'INV-2026-008', 3, 14, 'aml.analyst2@hsbc.com', '2026-03-16 12:00:00', NULL, NULL, 'HIGH', 'German-based Iranian national wired €45K to Tejarat Bank — Iranian state bank subject to EU/OFAC sanctions.'),
    (9, 'INV-2026-009', 20, 16, 'aml.analyst1@hsbc.com', '2026-03-21 17:00:00', NULL, NULL, 'MEDIUM', 'Atlas Foundation CHF 28K to Syria. Reviewing legitimacy of humanitarian exemption claim.'),
    (10, 'INV-2026-010', 1, 1, 'aml.analyst1@hsbc.com', '2026-03-04 09:00:00', '2026-03-10 11:00:00', 'NO_ACTION', 'MEDIUM', 'Three cash deposits of £9,500 / £9,400 / £8,900 reviewed. Customer provided salary slip and bonus documentation. Resolved — no action.'),
    (11, 'INV-2026-011', 16, 17, 'aml.analyst1@hsbc.com', '2026-03-18 17:00:00', NULL, NULL, 'HIGH', 'PEP (Andre Mobutu, DRC) transferred €88K to Rawbank DRC. SOF documents requested. Enhanced monitoring applied.'),
    (12, 'INV-2026-012', 10, 6, 'aml.analyst1@hsbc.com', '2026-03-12 18:00:00', NULL, NULL, 'HIGH', 'Layering investigation. Pacific Bridge Holdings received HKD 2.1M from China and transferred HKD 1.95M to Cayman same day.'),
    (13, 'INV-2026-013', 12, 7, 'aml.analyst1@hsbc.com', '2026-03-15 15:00:00', '2026-03-19 09:00:00', 'NO_ACTION', 'LOW', 'MXN 1.2M transfer by PEP to US. Confirmed property purchase with notarised title deeds. Closed — no action.'),
    (14, 'INV-2026-014', 19, 10, 'aml.analyst2@hsbc.com', '2026-03-13 13:00:00', NULL, NULL, 'HIGH', 'Cayman Islands trust → Russia wire corridor alert. Reviewing beneficial ownership structure of trust.'),
    (15, 'INV-2026-015', 6, 4, 'aml.analyst2@hsbc.com', '2026-03-25 10:00:00', NULL, NULL, 'MEDIUM', 'Third large inbound wire from UAE for Viktor Sokolov. Reviewing source of funds — customer claims real estate sales.'),
    (16, 'INV-2026-016', 5, 10, 'aml.analyst2@hsbc.com', '2026-03-13 11:30:00', NULL, NULL, 'MEDIUM', 'Greenleaf Trust Company Cayman Islands — KYC expired. Requesting UBO documentation and re-verification.'),
    (17, 'INV-2026-017', 18, 16, 'aml.analyst1@hsbc.com', '2026-03-21 16:15:00', NULL, NULL, 'MEDIUM', 'Atlas Charitable Foundation flagged for rapid offshore transfer. Reviewing grant disbursement approvals.'),
    (18, 'INV-2026-018', 17, 9, 'aml.senior@hsbc.com', '2026-03-13 11:00:00', NULL, NULL, 'HIGH', 'Trust wire to Russia escalated. Account under enhanced monitoring pending beneficial ownership disclosure.'),
    (19, 'INV-2026-019', 11, 19, 'aml.analyst1@hsbc.com', '2026-03-22 13:00:00', NULL, NULL, 'LOW', 'Single cash deposit £9,200. Reviewing for structuring pattern — no prior alerts. Customer is regular salaried employee.'),
    (20, 'INV-2026-020', 13, 3, 'aml.analyst1@hsbc.com', '2026-03-20 10:00:00', NULL, NULL, 'MEDIUM', 'Meridian Trading Ltd dormant 6 months then £95K payroll run. Reviewing business accounts and board resolutions.');
