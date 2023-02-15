ALTER TABLE sep24_transaction ADD amount_expected VARCHAR(255);

ALTER TABLE sep24_transaction ADD kyc_verified VARCHAR(255);

ALTER TABLE sep24_transaction ADD message VARCHAR(255);

ALTER TABLE sep24_transaction ADD more_info_url VARCHAR(255);

ALTER TABLE sep24_transaction ADD refund_memo VARCHAR(255);

ALTER TABLE sep24_transaction ADD refund_memo_type VARCHAR(255);

ALTER TABLE sep24_transaction ADD refunded BOOLEAN;

ALTER TABLE sep24_transaction ADD refunds JSON;

ALTER TABLE sep24_transaction ADD request_asset_code VARCHAR(255);

ALTER TABLE sep24_transaction ADD request_asset_issuer VARCHAR(255);

ALTER TABLE sep24_transaction ADD status_eta VARCHAR(255);

ALTER TABLE sep24_transaction ADD stellar_transactions JSON;

ALTER TABLE sep24_transaction ADD transfer_received_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sep24_transaction ADD updated_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sep24_refund_payment DROP CONSTRAINT fk_sep24_refund_payment_on_transaction;

DROP TABLE sep24_refund_payment CASCADE;

ALTER TABLE sep24_transaction DROP COLUMN sep_transaction_id;

ALTER TABLE sep24_transaction DROP COLUMN asset_code;

ALTER TABLE sep24_transaction DROP COLUMN asset_issuer;

ALTER TABLE sep24_transaction DROP COLUMN muxed_account;

ALTER TABLE sep24_transaction DROP COLUMN sep10account_memo;

ALTER TABLE sep24_transaction DROP COLUMN completed_at;

ALTER TABLE sep24_transaction DROP COLUMN started_at;

ALTER TABLE sep24_transaction ADD completed_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sep24_transaction ALTER COLUMN  id SET NOT NULL;

ALTER TABLE sep24_transaction ADD started_at TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE sep24_transaction ADD CONSTRAINT pk_sep24_transaction PRIMARY KEY (id);

CREATE TABLE sep31_historic (
    refunds VARCHAR(255)
);

INSERT INTO sep31_historic (refunds)
SELECT refunds
FROM sep31_transaction;

ALTER TABLE sep31_transaction DROP COLUMN refunds;

ALTER TABLE sep31_transaction ADD refunds JSON;

ALTER TABLE sep31_transaction ADD bank_account_type VARCHAR(255);
