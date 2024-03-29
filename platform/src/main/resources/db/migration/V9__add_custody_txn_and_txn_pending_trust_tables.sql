CREATE TABLE custody_transaction (
   id VARCHAR(255),
   sep_tx_id VARCHAR(255),
   external_tx_id VARCHAR(255),
   status VARCHAR(255),
   amount VARCHAR(255),
   amount_fee VARCHAR(255),
   asset VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   updated_at TIMESTAMP WITHOUT TIME ZONE,
   memo VARCHAR(255),
   memo_type VARCHAR(255),
   protocol VARCHAR(255),
   from_account VARCHAR(255),
   to_account VARCHAR(255),
   kind VARCHAR(255),
   reconciliation_attempt_count integer,
   type VARCHAR(255),
   CONSTRAINT pk_custody_transaction PRIMARY KEY (id)
);

CREATE TABLE transaction_pending_trust (
   id VARCHAR(255),
   asset VARCHAR(255),
   account VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   CONSTRAINT pk_transaction_pending_trust PRIMARY KEY (id)
);
