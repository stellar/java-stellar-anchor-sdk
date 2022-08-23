CREATE TABLE stellar_payment_observer_page_token
(
    id     VARCHAR(255) NOT NULL,
    cursor VARCHAR(255),
    CONSTRAINT pk_stellar_payment_observer_page_token PRIMARY KEY (id)
);

CREATE TABLE stellar_payment_observing_account
(
    account       VARCHAR(255) NOT NULL,
    last_observed TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_stellar_payment_observing_account PRIMARY KEY (account)
);

ALTER TABLE stellar_payment_observing_account
    ADD CONSTRAINT uc_stellar_payment_observing_account_account UNIQUE (account);

DROP TABLE stellar_account_page_token CASCADE;

ALTER TABLE sep31_transaction DROP COLUMN message;