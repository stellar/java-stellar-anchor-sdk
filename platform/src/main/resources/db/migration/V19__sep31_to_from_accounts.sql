ALTER TABLE sep31_transaction ADD from_account VARCHAR(255);
ALTER TABLE sep31_transaction RENAME COLUMN stellar_account_id TO to_account;