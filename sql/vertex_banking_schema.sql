-- ========================================================
-- VERTEX BANKING SYSTEM - RELATIONAL DATABASE SCHEMA (MySQL)
-- Customer Account & Transaction Management System
-- Group G2, Team G02-PID-17
-- ========================================================

CREATE DATABASE IF NOT EXISTS vertex_banking;
USE vertex_banking;

-- 1. Customers Table
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    dob DATE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Accounts Table
CREATE TABLE IF NOT EXISTS accounts (
    account_number VARCHAR(20) PRIMARY KEY,
    customer_id VARCHAR(20) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    balance DECIMAL(15, 2) NOT NULL CHECK (balance >= 0),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    daily_limit DECIMAL(15, 2) NOT NULL,
    daily_withdrawn_today DECIMAL(15, 2) NOT NULL DEFAULT 0.00,
    last_withdrawal_date DATE NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

-- 3. Transactions Table
CREATE TABLE IF NOT EXISTS transactions (
    txn_id VARCHAR(30) PRIMARY KEY,
    account_number VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    amount DECIMAL(15, 2) NOT NULL CHECK (amount > 0),
    txn_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    remarks VARCHAR(255),
    FOREIGN KEY (account_number) REFERENCES accounts(account_number) ON DELETE RESTRICT
);

-- Indices for performance
CREATE INDEX idx_accounts_customer ON accounts(customer_id);
CREATE INDEX idx_transactions_account ON transactions(account_number);
CREATE INDEX idx_transactions_date ON transactions(txn_date);

-- ========================================================
-- OFFICIAL DEMONSTRATION DATASET
-- Step 1: Register Asha (C101) & Rohan (C102)
-- Step 2: Open A501 (Savings, 50,000) & A502 (Current, 5,000)
-- ========================================================

INSERT INTO customers (customer_id, name, email, phone, dob) VALUES
('C101', 'Asha Sharma', 'asha@vertex.com', '9876543210', '2000-05-10')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO customers (customer_id, name, email, phone, dob) VALUES
('C102', 'Rohan Verma', 'rohan@vertex.com', '9876543211', '1999-08-22')
ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO accounts (account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date) VALUES
('A501', 'C101', 'SAVINGS', 50000.00, 'ACTIVE', 25000.00, 0.00, CURRENT_DATE)
ON DUPLICATE KEY UPDATE balance=VALUES(balance);

INSERT INTO accounts (account_number, customer_id, account_type, balance, status, daily_limit, daily_withdrawn_today, last_withdrawal_date) VALUES
('A502', 'C102', 'CURRENT', 5000.00, 'ACTIVE', 100000.00, 0.00, CURRENT_DATE)
ON DUPLICATE KEY UPDATE balance=VALUES(balance);

INSERT INTO transactions (txn_id, account_number, type, amount, txn_date, status, remarks) VALUES
('TXN-INIT-A501', 'A501', 'DEPOSIT', 50000.00, CURRENT_TIMESTAMP, 'SUCCESS', 'Opening Deposit'),
('TXN-INIT-A502', 'A502', 'DEPOSIT', 5000.00, CURRENT_TIMESTAMP, 'SUCCESS', 'Opening Deposit')
ON DUPLICATE KEY UPDATE status=VALUES(status);
