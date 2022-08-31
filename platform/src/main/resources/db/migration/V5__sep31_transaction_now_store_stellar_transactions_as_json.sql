ALTER TABLE sep31_transaction
    ADD stellar_transactions JSON;

ALTER TABLE stellar_transaction
    DROP CONSTRAINT fk_stellar_transaction_on_sep_31_transaction;

DROP TABLE stellar_transaction CASCADE;