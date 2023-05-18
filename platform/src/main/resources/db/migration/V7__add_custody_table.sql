CREATE TABLE custody_transaction (
   id VARCHAR(255),
   external_id VARCHAR(255),
   status VARCHAR(255),
   amount_in VARCHAR(255),
   amount_in_asset VARCHAR(255),
   amount_out VARCHAR(255),
   amount_out_asset VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   updated_at TIMESTAMP WITHOUT TIME ZONE,
   memo VARCHAR(255),
   memo_type VARCHAR(255),
   protocol VARCHAR(255),
   from_account VARCHAR(255),
   to_account VARCHAR(255),
   kind VARCHAR(255),
   reconciliation_attempt_count integer,
   CONSTRAINT pk_custody_transaction PRIMARY KEY (id)
);
