CREATE TABLE customer (
    customer_id         INT             PRIMARY KEY AUTO_INCREMENT,
    customer_ref        VARCHAR(15)     NOT NULL UNIQUE,   -- internal reference
    full_name           VARCHAR(100)    NOT NULL,
    date_of_birth       DATE,
    nationality         VARCHAR(2)         NOT NULL,          -- ISO 3166-1 alpha-2
    country_of_residence VARCHAR(2)        NOT NULL,
    customer_type       VARCHAR(20)     NOT NULL CHECK (customer_type IN ('INDIVIDUAL', 'CORPORATE', 'TRUST', 'CHARITY')),
    risk_rating         VARCHAR(10)     NOT NULL CHECK (risk_rating IN ('LOW', 'MEDIUM', 'HIGH')),  -- SANCTIONED deliberately not included as kyc_status and watchlist covers it, PEP not included is_pep with appropriate risk assigned is more readable
    kyc_status          VARCHAR(15)     NOT NULL CHECK (kyc_status IN ('VERIFIED', 'PENDING', 'EXPIRED', 'BLOCKED')),
    onboarded_date      DATE            NOT NULL,
    is_pep              BOOLEAN         NOT NULL DEFAULT FALSE,  -- Politically Exposed Person
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE
);