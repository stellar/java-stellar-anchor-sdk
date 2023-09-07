ALTER TABLE sep6_transaction ADD required_customer_info_message VARCHAR(255);
ALTER TABLE sep6_transaction ADD required_customer_info_updates JSON;
ALTER TABLE sep6_transaction ADD instructions JSON;

ALTER TABLE sep6_transaction DROP COLUMN required_info_updates;
ALTER TABLE sep6_transaction ADD required_info_updates JSON;