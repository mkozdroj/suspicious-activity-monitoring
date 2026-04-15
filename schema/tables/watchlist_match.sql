CREATE TABLE watchlist_match (
    match_id            INT             PRIMARY KEY AUTO_INCREMENT,
    txn_id              INT             NOT NULL,
    watchlist_id        INT             NOT NULL,
    match_type          VARCHAR(20)     NOT NULL CHECK (match_type IN ('NAME', 'ACCOUNT', 'COUNTRY', 'FUZZY_NAME')),
    match_score         DECIMAL(5,2)    NOT NULL CHECK (match_score BETWEEN 0.00 AND 100.00),
    matched_field       VARCHAR(50)     NOT NULL,          -- field that triggered match
    matched_value       VARCHAR(120)    NOT NULL,
    status              VARCHAR(15)     NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'FALSE_POSITIVE', 'CONFIRMED', 'ESCALATED')),
    reviewed_by         VARCHAR(60),
    reviewed_at         TIMESTAMP,
    FOREIGN KEY (txn_id) REFERENCES txn(txn_id),
    FOREIGN KEY (watchlist_id)   REFERENCES watchlist(watchlist_id)
);