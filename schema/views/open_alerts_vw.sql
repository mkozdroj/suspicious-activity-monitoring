
CREATE VIEW open_alerts_vw AS
SELECT
    a.alert_id,
    a.alert_ref,
    a.status AS alert_status,


    -- Transaction details
    t.txn_id,
    t.txn_ref,
    t.amount,
    t.currency,
    t.amount_usd,
    t.txn_date,
    t.counterparty_country,
    t.txn_type,
    t.direction,

    -- Customer details
    c.customer_id,
    c.customer_ref,
    c.full_name,
    c.date_of_birth,
    c.nationality,
    c.country_of_residence,
    c.customer_type,
    c.risk_rating,
    c.kyc_status,
    c.is_pep,
    c.is_active

FROM alert a
JOIN txn t
    ON a.txn_id = t.txn_id
JOIN customer c
    ON t.account_id = c.customer_id

WHERE a.status IN ('open', 'under_review');