CREATE TABLE watchlist (
    watchlist_id        INT             PRIMARY KEY AUTO_INCREMENT,
    list_type           VARCHAR(20)     NOT NULL CHECK (list_type IN ('OFAC', 'UN', 'EU', 'HMT', 'INTERPOL', 'INTERNAL', 'PEP')),
    entity_name         VARCHAR(120)    NOT NULL,
    entity_type         VARCHAR(20)     NOT NULL CHECK (entity_type IN ('INDIVIDUAL', 'ENTITY', 'VESSEL', 'AIRCRAFT')),
    country             CHAR(2),
    date_of_birth       DATE,
    reason              VARCHAR(200)    NOT NULL,
    listed_date         DATE            NOT NULL,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);