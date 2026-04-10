CREATE TABLE transaction (
    transaction_id      INT             PRIMARY KEY AUTO_INCREMENT,
    transaction_ref     VARCHAR(20)     NOT NULL UNIQUE,
    account_id          INT             NOT NULL,
    counterparty_account VARCHAR(30),                      -- external account
    counterparty_bank   VARCHAR(60),
    counterparty_country CHAR(2),
    transaction_type    VARCHAR(20)     NOT NULL CHECK (transaction_type IN ('WIRE', 'CASH', 'CARD', 'INTERNAL', 'CRYPTO', 'CHEQUE')),
    direction           CHAR(2)         NOT NULL CHECK (direction IN ('CR', 'DR')),   -- CR - in DR - out
    amount              DECIMAL(18,2)   NOT NULL CHECK (amount > 0),
    currency            CHAR(3)         NOT NULL,
    amount_usd          DECIMAL(18,2)   NOT NULL CHECK (amount_usd > 0),          -- normalised to USD
    transaction_date    DATE            NOT NULL,
    value_date          DATE            NOT NULL,
    status              VARCHAR(12)     NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'PENDING', 'REVERSED', 'FAILED')),
    description         VARCHAR(200),
    FOREIGN KEY (account_id) REFERENCES account(account_id),
    CONSTRAINT chk_value_date CHECK (value_date >= transaction_date)
);

CREATE INDEX idx_txn_account   ON transaction(account_id);
CREATE INDEX idx_txn_date      ON transaction(transaction_date);
CREATE INDEX idx_txn_amount    ON transaction(amount_usd);
CREATE INDEX idx_txn_cc        ON transaction(counterparty_country);