CREATE TABLE sep12_customer_id
(
    id        VARCHAR(255) NOT NULL,
    account   VARCHAR(255),
    memo      VARCHAR(255),
    memo_type VARCHAR(255),
    CONSTRAINT pk_sep12_customer_id PRIMARY KEY (id)
);