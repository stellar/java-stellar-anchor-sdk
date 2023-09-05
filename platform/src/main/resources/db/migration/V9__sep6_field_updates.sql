ALTER TABLE sep6_transaction DROP COLUMN how;
ALTER TABLE sep6_transaction ADD required_customer_info_message VARCHAR(255);
ALTER TABLE sep6_transaction ADD required_customer_info_updates VARCHAR(255);
ALTER TABLE sep6_transaction ADD instructions VARCHAR(255);