CREATE TABLE customer (
    cif             VARCHAR(9)    PRIMARY KEY,
    first_name      VARCHAR(60)   NOT NULL,
    last_name       VARCHAR(60)   NOT NULL,
    email           VARCHAR(120),
    phone           VARCHAR(20),
    ssn_last4       VARCHAR(4),
    segment         VARCHAR(20)   NOT NULL,
    kyc_status      VARCHAR(20)   NOT NULL,
    customer_since  DATE          NOT NULL
);

CREATE TABLE account (
    account_number        VARCHAR(12)    PRIMARY KEY,
    cif                   VARCHAR(9),
    product_code          VARCHAR(10)    NOT NULL,
    nickname              VARCHAR(60),
    status                VARCHAR(10)    NOT NULL,
    ledger_balance        DECIMAL(15,2)  NOT NULL,
    daily_transfer_limit  DECIMAL(15,2),
    opened_on             DATE           NOT NULL,
    CONSTRAINT fk_account_customer FOREIGN KEY (cif) REFERENCES customer (cif)
);

CREATE TABLE txn (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number   VARCHAR(12)    NOT NULL,
    posted_at        TIMESTAMP      NOT NULL,
    business_date    DATE           NOT NULL,
    txn_type         VARCHAR(6)     NOT NULL,
    channel          VARCHAR(10)    NOT NULL,
    amount           DECIMAL(15,2)  NOT NULL,
    running_balance  DECIMAL(15,2)  NOT NULL,
    description      VARCHAR(120)   NOT NULL,
    status           VARCHAR(10)    NOT NULL,
    transfer_id      VARCHAR(24),
    CONSTRAINT fk_txn_account FOREIGN KEY (account_number) REFERENCES account (account_number)
);
CREATE INDEX ix_txn_account_date ON txn (account_number, business_date);

CREATE SEQUENCE transfer_seq START WITH 230;

CREATE TABLE book_transfer (
    transfer_id      VARCHAR(24)    PRIMARY KEY,
    from_account     VARCHAR(12)    NOT NULL,
    to_account       VARCHAR(12)    NOT NULL,
    amount           DECIMAL(15,2)  NOT NULL,
    memo             VARCHAR(60),
    idempotency_key  VARCHAR(64),
    status           VARCHAR(10)    NOT NULL,
    initiated_by     VARCHAR(40)    NOT NULL,
    posted_at        TIMESTAMP      NOT NULL,
    business_date    DATE           NOT NULL
);

CREATE TABLE funds_hold (
    hold_id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_number   VARCHAR(12)    NOT NULL,
    amount           DECIMAL(15,2)  NOT NULL,
    reason           VARCHAR(20)    NOT NULL,
    placed_by        VARCHAR(40)    NOT NULL,
    release_on       DATE           NOT NULL,
    status           VARCHAR(10)    NOT NULL
);

CREATE TABLE app_user (
    username         VARCHAR(40)    PRIMARY KEY,
    password_hash    VARCHAR(64)    NOT NULL,
    role             VARCHAR(10)    NOT NULL,
    cif              VARCHAR(9),
    enabled          BOOLEAN        NOT NULL
);
