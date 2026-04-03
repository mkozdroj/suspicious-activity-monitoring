-- ============================================================
--  HSBC Graduate Programme 2026 — Project 04
--  Suspicious Activity Monitor (SAM) — DDL Schema
--
--  Compatible with PostgreSQL and MySQL 8+.
--  Run this file first, then load data files 01–08 in order.
-- ============================================================

DROP TABLE IF EXISTS watchlist_match;
DROP TABLE IF EXISTS watchlist;
DROP TABLE IF EXISTS investigation;
DROP TABLE IF EXISTS alert;
DROP TABLE IF EXISTS alert_rule;
DROP TABLE IF EXISTS transaction;
DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS customer;

-- ── customer ──────────────────────────────────────────────
CREATE TABLE customer (
    customer_id         INT             PRIMARY KEY,
    customer_ref        VARCHAR(15)     NOT NULL UNIQUE,   -- internal reference
    full_name           VARCHAR(100)    NOT NULL,
    date_of_birth       DATE,
    nationality         CHAR(2)         NOT NULL,          -- ISO 3166-1 alpha-2
    country_of_residence CHAR(2)        NOT NULL,
    customer_type       VARCHAR(20)     NOT NULL,          -- INDIVIDUAL / CORPORATE / TRUST / CHARITY
    risk_rating         VARCHAR(10)     NOT NULL,          -- LOW / MEDIUM / HIGH / PEP / SANCTIONED
    kyc_status          VARCHAR(15)     NOT NULL,          -- VERIFIED / PENDING / EXPIRED / BLOCKED
    onboarded_date      DATE            NOT NULL,
    is_pep              BOOLEAN         NOT NULL DEFAULT FALSE,  -- Politically Exposed Person
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ── account ───────────────────────────────────────────────
CREATE TABLE account (
    account_id          INT             PRIMARY KEY,
    account_number      VARCHAR(20)     NOT NULL UNIQUE,
    customer_id         INT             NOT NULL,
    account_type        VARCHAR(20)     NOT NULL,          -- CURRENT / SAVINGS / TRADING / CUSTODY / CORRESPONDENT
    currency            CHAR(3)         NOT NULL,
    balance             DECIMAL(18,2)   NOT NULL DEFAULT 0.00,
    opened_date         DATE            NOT NULL,
    status              VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE / FROZEN / CLOSED / RESTRICTED
    branch_code         VARCHAR(10)     NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

CREATE INDEX idx_account_customer ON account(customer_id);

-- ── transaction ───────────────────────────────────────────
CREATE TABLE transaction (
    transaction_id      INT             PRIMARY KEY,
    transaction_ref     VARCHAR(20)     NOT NULL UNIQUE,
    account_id          INT             NOT NULL,
    counterparty_account VARCHAR(30),                      -- external account
    counterparty_bank   VARCHAR(60),
    counterparty_country CHAR(2),
    transaction_type    VARCHAR(20)     NOT NULL,          -- WIRE / CASH / CARD / INTERNAL / CRYPTO / CHEQUE
    direction           CHAR(2)         NOT NULL,          -- CR (credit in) / DR (debit out)
    amount              DECIMAL(18,2)   NOT NULL,
    currency            CHAR(3)         NOT NULL,
    amount_usd          DECIMAL(18,2)   NOT NULL,          -- normalised to USD
    transaction_date    DATE            NOT NULL,
    value_date          DATE            NOT NULL,
    status              VARCHAR(12)     NOT NULL DEFAULT 'COMPLETED',
    description         VARCHAR(200),
    FOREIGN KEY (account_id) REFERENCES account(account_id)
);

CREATE INDEX idx_txn_account   ON transaction(account_id);
CREATE INDEX idx_txn_date      ON transaction(transaction_date);
CREATE INDEX idx_txn_amount    ON transaction(amount_usd);
CREATE INDEX idx_txn_cc        ON transaction(counterparty_country);

-- ── alert_rule ────────────────────────────────────────────
CREATE TABLE alert_rule (
    rule_id             INT             PRIMARY KEY,
    rule_code           VARCHAR(20)     NOT NULL UNIQUE,
    rule_name           VARCHAR(100)    NOT NULL,
    rule_category       VARCHAR(30)     NOT NULL,          -- STRUCTURING / VELOCITY / WATCHLIST / GEOGRAPHY / PATTERN
    description         VARCHAR(255)    NOT NULL,
    threshold_amount    DECIMAL(18,2),                     -- USD threshold if applicable
    threshold_count     INT,                               -- transaction count if applicable
    lookback_days       INT             NOT NULL DEFAULT 30,
    severity            VARCHAR(10)     NOT NULL,          -- LOW / MEDIUM / HIGH / CRITICAL
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ── alert ─────────────────────────────────────────────────
CREATE TABLE alert (
    alert_id            INT             PRIMARY KEY,
    alert_ref           VARCHAR(15)     NOT NULL UNIQUE,
    rule_id             INT             NOT NULL,
    account_id          INT             NOT NULL,
    transaction_id      INT,                               -- triggering transaction (if applicable)
    triggered_at        TIMESTAMP       NOT NULL,
    alert_score         SMALLINT        NOT NULL,          -- 0–100 risk score
    status              VARCHAR(15)     NOT NULL DEFAULT 'OPEN',  -- OPEN / UNDER_REVIEW / ESCALATED / CLOSED / SAR_FILED
    assigned_to         VARCHAR(60),
    notes               VARCHAR(500),
    FOREIGN KEY (rule_id)        REFERENCES alert_rule(rule_id),
    FOREIGN KEY (account_id)     REFERENCES account(account_id),
    FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id)
);

CREATE INDEX idx_alert_account ON alert(account_id);
CREATE INDEX idx_alert_status  ON alert(status);

-- ── investigation ─────────────────────────────────────────
CREATE TABLE investigation (
    investigation_id    INT             PRIMARY KEY,
    investigation_ref   VARCHAR(15)     NOT NULL UNIQUE,
    alert_id            INT             NOT NULL,
    customer_id         INT             NOT NULL,
    opened_by           VARCHAR(60)     NOT NULL,
    opened_at           TIMESTAMP       NOT NULL,
    closed_at           TIMESTAMP,
    outcome             VARCHAR(20),                       -- SAR_FILED / NO_ACTION / ACCOUNT_CLOSED / ESCALATED / MONITORING
    priority            VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM',  -- LOW / MEDIUM / HIGH / URGENT
    findings            VARCHAR(1000),
    FOREIGN KEY (alert_id)    REFERENCES alert(alert_id),
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

-- ── watchlist ─────────────────────────────────────────────
CREATE TABLE watchlist (
    watchlist_id        INT             PRIMARY KEY,
    list_type           VARCHAR(20)     NOT NULL,          -- OFAC / UN / EU / HMT / INTERPOL / INTERNAL / PEP
    entity_name         VARCHAR(120)    NOT NULL,
    entity_type         VARCHAR(20)     NOT NULL,          -- INDIVIDUAL / ENTITY / VESSEL / AIRCRAFT
    country             CHAR(2),
    date_of_birth       DATE,
    reason              VARCHAR(200)    NOT NULL,
    listed_date         DATE            NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);

-- ── watchlist_match ───────────────────────────────────────
CREATE TABLE watchlist_match (
    match_id            INT             PRIMARY KEY,
    transaction_id      INT             NOT NULL,
    watchlist_id        INT             NOT NULL,
    match_type          VARCHAR(20)     NOT NULL,          -- NAME / ACCOUNT / COUNTRY / FUZZY_NAME
    match_score         DECIMAL(5,2)    NOT NULL,          -- 0.00–100.00 confidence
    matched_field       VARCHAR(50)     NOT NULL,          -- field that triggered match
    matched_value       VARCHAR(120)    NOT NULL,
    status              VARCHAR(15)     NOT NULL DEFAULT 'PENDING',  -- PENDING / FALSE_POSITIVE / CONFIRMED / ESCALATED
    reviewed_by         VARCHAR(60),
    reviewed_at         TIMESTAMP,
    FOREIGN KEY (transaction_id) REFERENCES transaction(transaction_id),
    FOREIGN KEY (watchlist_id)   REFERENCES watchlist(watchlist_id)
);
