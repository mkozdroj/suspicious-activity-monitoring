CREATE TABLE alert (
    alert_id            INT             PRIMARY KEY AUTO_INCREMENT,
    alert_ref           VARCHAR(15)     NOT NULL UNIQUE,
    rule_id             INT             NOT NULL,
    account_id          INT             NOT NULL,
    txn_id              INT,
    triggered_at        TIMESTAMP       NOT NULL,
    alert_score         SMALLINT        NOT NULL CHECK (alert_score BETWEEN 0 AND 100),
    status              VARCHAR(15)     NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'UNDER_REVIEW', 'ESCALATED', 'CLOSED', 'FALSE_POSITIVE', 'SAR_FILED')),
    assigned_to         VARCHAR(60),
    notes               VARCHAR(500),
    FOREIGN KEY (rule_id)        REFERENCES alert_rule(rule_id),
    FOREIGN KEY (account_id)     REFERENCES account(account_id),
    FOREIGN KEY (txn_id) REFERENCES txn(txn_id)
);

CREATE INDEX idx_alert_account ON alert(account_id);
CREATE INDEX idx_alert_status  ON alert(status);