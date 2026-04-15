CREATE TABLE account (
    account_id          INT             PRIMARY KEY AUTO_INCREMENT,
    account_number      VARCHAR(40)     NOT NULL UNIQUE,
    customer_id         INT             NOT NULL,
    account_type        VARCHAR(20)     NOT NULL CHECK (account_type IN ('CURRENT', 'SAVINGS', 'TRADING', 'CUSTODY', 'CORRESPONDENT')),
    currency            VARCHAR(3)         NOT NULL,
    balance             DECIMAL(18,2)   NOT NULL DEFAULT 0.00,
    opened_date         DATE            NOT NULL,
    status              VARCHAR(10)     NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED', 'RESTRICTED')),
    branch_code         VARCHAR(10)     NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id)
);

CREATE INDEX idx_account_customer ON account(customer_id);