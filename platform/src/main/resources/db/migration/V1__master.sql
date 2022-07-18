CREATE SEQUENCE  IF NOT EXISTS hibernate_sequence START WITH 1 INCREMENT BY 1;

CREATE TABLE exchange_quote (
  id VARCHAR(255) NOT NULL,
   expires_at TIMESTAMP WITHOUT TIME ZONE,
   price VARCHAR(255),
   total_price VARCHAR(255),
   sell_asset VARCHAR(255),
   sell_amount VARCHAR(255),
   sell_delivery_method VARCHAR(255),
   buy_asset VARCHAR(255),
   buy_amount VARCHAR(255),
   buy_delivery_method VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   creator_account_id VARCHAR(255),
   creator_memo VARCHAR(255),
   creator_memo_type VARCHAR(255),
   transaction_id VARCHAR(255),
   fee VARCHAR(1023),
   CONSTRAINT pk_exchange_quote PRIMARY KEY (id)
);

CREATE TABLE sep24_refund_payment (
  sep24_refund_payment_id UUID NOT NULL,
   transaction_id UUID NOT NULL,
   id VARCHAR(255),
   id_type VARCHAR(255),
   amount VARCHAR(255),
   fee VARCHAR(255),
   CONSTRAINT pk_sep24_refund_payment PRIMARY KEY (sep24_refund_payment_id)
);

CREATE TABLE sep24_transaction (
  sep_transaction_id UUID NOT NULL,
   id VARCHAR(255),
   transaction_id VARCHAR(255),
   stellar_transaction_id VARCHAR(255),
   external_transaction_id VARCHAR(255),
   status VARCHAR(255),
   kind VARCHAR(255),
   started_at BIGINT,
   completed_at BIGINT,
   asset_code VARCHAR(255),
   asset_issuer VARCHAR(255),
   sep10account VARCHAR(255),
   sep10account_memo VARCHAR(255),
   withdraw_anchor_account VARCHAR(255),
   from_account VARCHAR(255),
   to_account VARCHAR(255),
   memo_type VARCHAR(255),
   memo VARCHAR(255),
   client_domain VARCHAR(255),
   claimable_balance_supported BOOLEAN,
   amount_in VARCHAR(255),
   amount_out VARCHAR(255),
   amount_fee VARCHAR(255),
   amount_in_asset VARCHAR(255),
   amount_out_asset VARCHAR(255),
   amount_fee_asset VARCHAR(255),
   muxed_account VARCHAR(255),
   CONSTRAINT pk_sep24_transaction PRIMARY KEY (sep_transaction_id)
);

CREATE TABLE sep31_transaction (
  id VARCHAR(255) NOT NULL,
   status VARCHAR(255),
   status_eta BIGINT,
   amount_in VARCHAR(255),
   amount_in_asset VARCHAR(255),
   amount_out VARCHAR(255),
   amount_out_asset VARCHAR(255),
   amount_fee VARCHAR(255),
   amount_fee_asset VARCHAR(255),
   stellar_account_id VARCHAR(255),
   stellar_memo VARCHAR(255),
   stellar_memo_type VARCHAR(255),
   started_at TIMESTAMP WITHOUT TIME ZONE,
   completed_at TIMESTAMP WITHOUT TIME ZONE,
   stellar_transaction_id VARCHAR(255),
   external_transaction_id VARCHAR(255),
   required_info_message VARCHAR(255),
   quote_id VARCHAR(255),
   client_domain VARCHAR(255),
   refunded BOOLEAN,
   updated_at TIMESTAMP WITHOUT TIME ZONE,
   transfer_received_at TIMESTAMP WITHOUT TIME ZONE,
   message VARCHAR(255),
   amount_expected VARCHAR(255),
   fields VARCHAR(255),
   required_info_updates VARCHAR(255),
   refunds VARCHAR(255),
   CONSTRAINT pk_sep31_transaction PRIMARY KEY (id)
);

CREATE TABLE stellar_account_page_token (
  account_id VARCHAR(255) NOT NULL,
   cursor VARCHAR(255),
   CONSTRAINT pk_stellar_account_page_token PRIMARY KEY (account_id)
);

CREATE TABLE stellar_transaction (
  id BIGINT NOT NULL,
   sep_31_transaction_id VARCHAR(255),
   memo VARCHAR(255),
   memo_type VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   envelope VARCHAR(255),
   payment VARCHAR(255),
   CONSTRAINT pk_stellar_transaction PRIMARY KEY (id)
);

ALTER TABLE sep24_refund_payment ADD CONSTRAINT FK_SEP24_REFUND_PAYMENT_ON_TRANSACTION FOREIGN KEY (transaction_id) REFERENCES sep24_transaction (sep_transaction_id);

ALTER TABLE stellar_transaction ADD CONSTRAINT FK_STELLAR_TRANSACTION_ON_SEP_31_TRANSACTION FOREIGN KEY (sep_31_transaction_id) REFERENCES sep31_transaction (id);