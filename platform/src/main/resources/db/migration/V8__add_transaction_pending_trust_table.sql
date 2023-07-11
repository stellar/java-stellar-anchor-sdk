CREATE TABLE transaction_pending_trust (
   id VARCHAR(255),
   asset VARCHAR(255),
   account VARCHAR(255),
   created_at TIMESTAMP WITHOUT TIME ZONE,
   reconciliation_attempt_count integer,
   CONSTRAINT pk_transaction_pending_trust PRIMARY KEY (id)
);
