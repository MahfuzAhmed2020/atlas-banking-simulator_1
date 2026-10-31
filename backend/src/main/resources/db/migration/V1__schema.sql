CREATE TABLE users (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 email VARCHAR(190) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL,
 full_name VARCHAR(150) NOT NULL,
 phone VARCHAR(30),
 street VARCHAR(220), city VARCHAR(90), state_code VARCHAR(2), zip_code VARCHAR(10), county_fips VARCHAR(5),
 address_verified BOOLEAN NOT NULL DEFAULT FALSE,
 role VARCHAR(20) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 email_verified BOOLEAN NOT NULL DEFAULT FALSE,
 phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
 referral_code VARCHAR(32) NOT NULL UNIQUE,
 referred_by BIGINT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (referred_by) REFERENCES users(id)
);
CREATE TABLE bank_accounts (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 account_number VARCHAR(24) NOT NULL UNIQUE,
 account_type VARCHAR(24) NOT NULL,
 status VARCHAR(20) NOT NULL,
 balance DECIMAL(18,2) NOT NULL DEFAULT 0,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (user_id) REFERENCES users(id), INDEX idx_account_user (user_id)
);
CREATE TABLE account_applications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL, account_type VARCHAR(24) NOT NULL,
 status VARCHAR(20) NOT NULL, reviewed_by BIGINT, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE journal_entries (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 reference_key VARCHAR(90) NOT NULL UNIQUE,
 description VARCHAR(240) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
CREATE TABLE ledger_postings (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 journal_id BIGINT NOT NULL,
 ledger_account VARCHAR(80) NOT NULL,
 amount DECIMAL(18,2) NOT NULL,
 FOREIGN KEY (journal_id) REFERENCES journal_entries(id), INDEX idx_postings_account (ledger_account)
);
CREATE TABLE check_deposits (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL, account_id BIGINT NOT NULL,
 check_number VARCHAR(45) NOT NULL, amount DECIMAL(18,2) NOT NULL,
 instant BOOLEAN NOT NULL,
 fee DECIMAL(18,2) NOT NULL DEFAULT 0,
 status VARCHAR(25) NOT NULL,
 front_path VARCHAR(220) NOT NULL, back_path VARCHAR(220) NOT NULL,
 image_hash VARCHAR(64) NOT NULL,
 available_on DATE NULL, reviewed_by BIGINT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 UNIQUE KEY uq_check_account_number (account_id,check_number),
 UNIQUE KEY uq_check_hash (image_hash),
 FOREIGN KEY (user_id) REFERENCES users(id), FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE transfers (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 from_account_id BIGINT NOT NULL, to_account_id BIGINT NOT NULL,
 amount DECIMAL(18,2) NOT NULL, status VARCHAR(25) NOT NULL,
 request_key VARCHAR(90) NOT NULL UNIQUE,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (from_account_id) REFERENCES bank_accounts(id), FOREIGN KEY (to_account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE direct_deposits (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 account_id BIGINT NOT NULL,
 amount DECIMAL(18,2) NOT NULL,
 employer VARCHAR(150) NOT NULL,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE cash_advances (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 account_id BIGINT NOT NULL, amount DECIMAL(18,2) NOT NULL,
 repaid DECIMAL(18,2) NOT NULL DEFAULT 0,
 status VARCHAR(25) NOT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE referrals (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 referrer_id BIGINT NOT NULL, referred_id BIGINT NOT NULL UNIQUE,
 status VARCHAR(25) NOT NULL, bonus_amount DECIMAL(18,2) NOT NULL DEFAULT 500,
 FOREIGN KEY (referrer_id) REFERENCES users(id), FOREIGN KEY (referred_id) REFERENCES users(id)
);
CREATE TABLE bill_payments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 account_id BIGINT NOT NULL,
 biller VARCHAR(160) NOT NULL,
 reference VARCHAR(80) NOT NULL,
 amount DECIMAL(18,2) NOT NULL, scheduled_on DATE NOT NULL,
 status VARCHAR(25) NOT NULL, request_key VARCHAR(90) NOT NULL UNIQUE,
 FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE loans (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 name VARCHAR(120) NOT NULL,
 amount_due DECIMAL(18,2) NOT NULL,
 due_on DATE NOT NULL,
 status VARCHAR(25) NOT NULL DEFAULT 'ACTIVE',
 FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE fema_disasters (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 disaster_number INTEGER NOT NULL, county_fips VARCHAR(5) NOT NULL,
 declaration_date DATE NOT NULL,
 UNIQUE KEY uq_fema_disaster_county (disaster_number, county_fips)
);
CREATE TABLE deferrals (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 loan_id BIGINT NOT NULL, disaster_number INTEGER NOT NULL,
 original_due DATE NOT NULL, revised_due DATE NOT NULL,
 status VARCHAR(25) NOT NULL,
 UNIQUE KEY uq_deferral_loan_disaster (loan_id,disaster_number),
 FOREIGN KEY (loan_id) REFERENCES loans(id)
);
CREATE TABLE alert_preferences (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 account_id BIGINT NOT NULL UNIQUE,
 low_balance DECIMAL(18,2) NOT NULL DEFAULT 100,
 large_transaction DECIMAL(18,2) NOT NULL DEFAULT 500,
 email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
 FOREIGN KEY (account_id) REFERENCES bank_accounts(id)
);
CREATE TABLE notifications (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, user_id BIGINT NOT NULL,
 title VARCHAR(130) NOT NULL, message VARCHAR(800) NOT NULL,
 read_flag BOOLEAN NOT NULL DEFAULT FALSE,
 created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
 FOREIGN KEY (user_id) REFERENCES users(id), INDEX idx_notification_user (user_id)
);
CREATE TABLE one_time_codes (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 user_id BIGINT NOT NULL,
 code_hash VARCHAR(100) NOT NULL, purpose VARCHAR(30) NOT NULL,
 expires_at TIMESTAMP(6) NOT NULL,
 attempts INTEGER NOT NULL DEFAULT 0, used BOOLEAN NOT NULL DEFAULT FALSE,
 FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE TABLE customer_assignments (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 employee_id BIGINT NOT NULL, customer_id BIGINT NOT NULL,
 UNIQUE KEY uq_employee_customer (employee_id,customer_id),
 FOREIGN KEY (employee_id) REFERENCES users(id), FOREIGN KEY (customer_id) REFERENCES users(id)
);
CREATE TABLE audit_events (
 id BIGINT PRIMARY KEY AUTO_INCREMENT, actor_id BIGINT NULL,
 action VARCHAR(90) NOT NULL, target_type VARCHAR(50) NOT NULL,
 target_id VARCHAR(100) NOT NULL, created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
