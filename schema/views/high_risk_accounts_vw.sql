CREATE VIEW high_risk_accounts_vw AS
SELECT DISTINCT
    c.customer_id,
    c.customer_ref,
    c.full_name,
    c.risk_rating,

    -- Account (from transactions)
    t.account_id,

    -- Alert info
    a.alert_id,
    a.status AS alert_status,
    a.triggered_at AS alert_date

FROM customer c
JOIN txn t
    ON c.customer_id = t.account_id
JOIN alert a
    ON t.txn_id = a.txn_id

WHERE c.risk_rating = 'High'
  AND a.triggered_at >= CURRENT_DATE - INTERVAL 30 DAY;