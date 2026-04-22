CREATE TABLE txn (
    txn_id               INT             PRIMARY KEY AUTO_INCREMENT,
    txn_ref              VARCHAR(20)     NOT NULL UNIQUE,
    account_id           INT             NOT NULL,
    counterparty_account VARCHAR(30),
    counterparty_bank    VARCHAR(60),
    counterparty_country VARCHAR(2),
    txn_type             VARCHAR(20)     NOT NULL CHECK (txn_type IN ('WIRE', 'CASH', 'CARD', 'INTERNAL', 'CRYPTO', 'CHEQUE')),
    direction            VARCHAR(2)         NOT NULL CHECK (direction IN ('CR', 'DR')),
    amount               DECIMAL(18,2)   NOT NULL CHECK (amount > 0),
    currency             VARCHAR(3)         NOT NULL,
    amount_usd           DECIMAL(18,2)   NOT NULL CHECK (amount_usd > 0),
    txn_date             DATE            NOT NULL,
    value_date           DATE            NOT NULL,
    status               VARCHAR(12)     NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED', 'PENDING', 'SCREENED', 'BLOCKED', 'REVERSED', 'FAILED')),
    description          VARCHAR(200),
    FOREIGN KEY (account_id) REFERENCES account(account_id),
    CONSTRAINT chk_value_date CHECK (value_date >= txn_date)
);

CREATE INDEX idx_txn_account   ON txn(account_id);
CREATE INDEX idx_txn_date      ON txn(txn_date);
CREATE INDEX idx_txn_amount    ON txn(amount_usd);
CREATE INDEX idx_txn_cc        ON txn(counterparty_country);