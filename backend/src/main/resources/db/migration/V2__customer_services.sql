CREATE TABLE customer_settings (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL UNIQUE,
 language VARCHAR(12) NOT NULL DEFAULT 'en', accessibility BOOLEAN NOT NULL DEFAULT FALSE,
 marketing BOOLEAN NOT NULL DEFAULT FALSE, FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE bank_cards (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 last_four VARCHAR(4) NOT NULL, kind VARCHAR(12) NOT NULL, status VARCHAR(20) NOT NULL,
 spending_limit DECIMAL(18,2) NOT NULL, atm_limit DECIMAL(18,2) NOT NULL,
 online_enabled BOOLEAN NOT NULL DEFAULT TRUE, international_enabled BOOLEAN NOT NULL DEFAULT FALSE,
 pin_hash VARCHAR(100), FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE service_requests (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, account_id BIGINT,
 kind VARCHAR(40) NOT NULL, details VARCHAR(2000) NOT NULL, status VARCHAR(20) NOT NULL,
 review_notes VARCHAR(2000), reviewed_by BIGINT, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(account_id) REFERENCES bank_accounts(id),
 FOREIGN KEY(reviewed_by) REFERENCES users(id)
);
CREATE TABLE support_messages (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, customer_id BIGINT NOT NULL, sender_id BIGINT NOT NULL,
 subject VARCHAR(160) NOT NULL, body VARCHAR(4000) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(customer_id) REFERENCES users(id), FOREIGN KEY(sender_id) REFERENCES users(id)
);
CREATE TABLE saved_billers (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, name VARCHAR(160) NOT NULL,
 reference VARCHAR(160) NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE transfer_schedules (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, from_account_id BIGINT NOT NULL,
 to_account_id BIGINT NOT NULL, amount DECIMAL(18,2) NOT NULL, next_date DATE NOT NULL,
 frequency VARCHAR(12) NOT NULL, status VARCHAR(20) NOT NULL, last_error VARCHAR(240),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(from_account_id) REFERENCES bank_accounts(id),
 FOREIGN KEY(to_account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE banking_products (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, code VARCHAR(24) NOT NULL UNIQUE, name VARCHAR(100) NOT NULL,
 enabled BOOLEAN NOT NULL DEFAULT TRUE
);
INSERT INTO banking_products(code,name) VALUES ('CHECKING','Checking'),('SAVINGS','Savings'),
 ('BUSINESS_CHECKING','Business checking'),('BUSINESS_SAVINGS','Business savings'),
 ('MONEY_MARKET','Money market'),('CD','Certificate of deposit'),('CREDIT_CARD','Credit card'),
 ('PERSONAL_LOAN','Personal loan'),('AUTO_LOAN','Auto loan'),('MORTGAGE','Mortgage'),('JOINT','Joint account');
UPDATE banking_products SET enabled=FALSE WHERE code IN ('CREDIT_CARD','PERSONAL_LOAN','AUTO_LOAN','MORTGAGE','JOINT');
CREATE TABLE bank_documents (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 kind VARCHAR(40) NOT NULL, period_start DATE NOT NULL, period_end DATE NOT NULL,
 snapshot MEDIUMTEXT NOT NULL, pdf_data MEDIUMBLOB NOT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(account_id) REFERENCES bank_accounts(id),
 UNIQUE(account_id,kind,period_start,period_end)
);
CREATE TABLE document_deliveries (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, document_id BIGINT NOT NULL, status VARCHAR(20) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6), FOREIGN KEY(document_id) REFERENCES bank_documents(id)
);
ALTER TABLE bill_payments ADD COLUMN frequency VARCHAR(12) NOT NULL DEFAULT 'ONCE';
ALTER TABLE bill_payments ADD COLUMN parent_id BIGINT;
CREATE INDEX idx_schedule_due ON transfer_schedules(status,next_date);
CREATE INDEX idx_support_customer ON support_messages(customer_id,id);
CREATE INDEX idx_requests_customer ON service_requests(user_id,status);
