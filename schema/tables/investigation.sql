CREATE TABLE investigation (
    investigation_id    INT             PRIMARY KEY AUTO_INCREMENT,
    investigation_ref   VARCHAR(15)     NOT NULL UNIQUE,
    alert_id            INT             NOT NULL UNIQUE,
    customer_id         INT             NOT NULL,
    opened_by           VARCHAR(60)     NOT NULL,
    opened_at           TIMESTAMP       NOT NULL,
    closed_at           TIMESTAMP,
    outcome             VARCHAR(20),    CHECK (outcome IN ('SAR_FILED', 'NO_ACTION', 'ACCOUNT_CLOSED', 'ESCALATED', 'MONITORING')),
    priority            VARCHAR(10)     NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    findings            VARCHAR(500),
    FOREIGN KEY (alert_id)    REFERENCES alert(alert_id),
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);