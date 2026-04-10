CREATE TABLE alert_rule (
    rule_id             INT             PRIMARY KEY AUTO_INCREMENT,
    rule_code           VARCHAR(20)     NOT NULL UNIQUE,
    rule_name           VARCHAR(100)    NOT NULL,
    rule_category       VARCHAR(30)     NOT NULL CHECK(rule_category IN ('STRUCTURING', 'SMURFING', 'VELOCITY', 'WATCHLIST', 'GEOGRAPHY', 'PATTERN')),
    description         VARCHAR(255)    NOT NULL,
    threshold_amount    DECIMAL(18,2),                     -- USD threshold if applicable
    threshold_count     INT,                               -- transaction count if applicable
    lookback_days       INT             DEFAULT 30,
    severity            VARCHAR(10)     NOT NULL CHECK (severity IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);