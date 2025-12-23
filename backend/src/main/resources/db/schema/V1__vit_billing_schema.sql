/*
    # VIT EnergySuite - Consolidated Database Schema

  This Flyway migration file contains the complete relational schema
  required to support all core operations: authentication, customer and
  account management, tariff configuration, meter readings, bill
  generation, payments, complaints, auditing, subsidies, and late fee
  policies.

  The schema is designed for MySQL 8.0+ and is idempotent thanks to the
  IF NOT EXISTS guards. Sample seed data is provided for essential
  bootstrap records (admin user, tariff definitions, charges, subsidies,
  and late fee policies).
*/

-- ------------------------------------------------------------------
--  AUTHENTICATION & AUDIT TABLES
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'CUSTOMER') NOT NULL DEFAULT 'CUSTOMER',
    is_active BOOLEAN DEFAULT TRUE,
    phone_number VARCHAR(15),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
--  CUSTOMER & ACCOUNT TABLES
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS customers (
    customer_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    customer_number VARCHAR(50) UNIQUE NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone_number VARCHAR(15) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(50),
    state VARCHAR(50) DEFAULT 'Maharashtra',
    pincode VARCHAR(10),
    aadhar_number VARCHAR(20) UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_customer_number (customer_number),
    INDEX idx_user_id (user_id),
    INDEX idx_phone (phone_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS accounts (
    account_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    account_number VARCHAR(50) UNIQUE NOT NULL,
    meter_number VARCHAR(50) UNIQUE NOT NULL,
    connection_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'AGRICULTURAL') NOT NULL DEFAULT 'RESIDENTIAL',
    sanctioned_load DECIMAL(10,2) NOT NULL,
    connection_date DATE NOT NULL,
    installation_address TEXT NOT NULL,
    tariff_category VARCHAR(20) NOT NULL DEFAULT 'LT-I',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    INDEX idx_account_number (account_number),
    INDEX idx_meter_number (meter_number),
    INDEX idx_customer_id (customer_id),
    INDEX idx_connection_type (connection_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
--  TARIFFS, SLABS & CHARGES
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS tariff_master (
    tariff_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tariff_code VARCHAR(20) UNIQUE NOT NULL,
    tariff_name VARCHAR(100) NOT NULL,
    connection_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'AGRICULTURAL') NOT NULL,
    description TEXT,
    fixed_charge DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    meter_rent DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    effective_from DATE NOT NULL,
    effective_to DATE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tariff_code (tariff_code),
    INDEX idx_connection_type (connection_type),
    INDEX idx_effective_dates (effective_from, effective_to)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tariff_slabs (
    slab_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tariff_id BIGINT NOT NULL,
    slab_number INT NOT NULL,
    min_units INT NOT NULL,
    max_units INT,
    rate_per_unit DECIMAL(10,4) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tariff_id) REFERENCES tariff_master(tariff_id) ON DELETE CASCADE,
    INDEX idx_tariff_id (tariff_id),
    UNIQUE KEY unique_slab (tariff_id, slab_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS additional_charges (
    charge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    charge_name VARCHAR(100) NOT NULL,
    charge_type ENUM('PERCENTAGE', 'FIXED') NOT NULL,
    charge_value DECIMAL(10,4) NOT NULL,
    applicable_to VARCHAR(255) DEFAULT 'ALL',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_charge_name (charge_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS subsidy_rules (
    rule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tariff_code VARCHAR(20) NOT NULL,
    connection_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'AGRICULTURAL') NOT NULL,
    max_units INT,
    per_unit_subsidy DECIMAL(10,4) DEFAULT 0.0000,
    percentage_subsidy DECIMAL(10,4) DEFAULT 0.0000,
    fixed_subsidy DECIMAL(10,2) DEFAULT 0.00,
    max_benefit DECIMAL(10,2) DEFAULT 0.00,
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_subsidy_tariff (tariff_code, connection_type, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS late_fee_policies (
    policy_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    connection_type ENUM('RESIDENTIAL', 'COMMERCIAL', 'INDUSTRIAL', 'AGRICULTURAL') NOT NULL,
    standard_due_days INT NOT NULL DEFAULT 15,
    grace_period_days INT NOT NULL DEFAULT 3,
    daily_rate_percentage DECIMAL(10,4) DEFAULT 0.0000,
    flat_fee DECIMAL(10,2) DEFAULT 0.00,
    max_late_fee DECIMAL(10,2),
    is_active BOOLEAN DEFAULT TRUE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_policy_connection (connection_type, effective_from)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
--  METER READINGS & BILLING''`"'''''''''"
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS meter_readings (
    reading_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    reading_date DATE NOT NULL,
    billing_month VARCHAR(7) NOT NULL,
    previous_reading INT NOT NULL DEFAULT 0,
    current_reading INT NOT NULL,
    units_consumed INT GENERATED ALWAYS AS (current_reading - previous_reading) STORED,
    reading_type ENUM('ACTUAL', 'ESTIMATED') NOT NULL DEFAULT 'ACTUAL',
    recorded_by BIGINT,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (recorded_by) REFERENCES users(user_id) ON DELETE SET NULL,
    UNIQUE KEY unique_reading (account_id, billing_month),
    INDEX idx_account_month (account_id, billing_month),
    INDEX idx_reading_date (reading_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS bills (
    bill_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    reading_id BIGINT NOT NULL,
    invoice_number VARCHAR(50) UNIQUE NOT NULL,
    bill_month VARCHAR(7) NOT NULL,
    bill_date DATE NOT NULL,
    due_date DATE NOT NULL,
    units_consumed INT NOT NULL,
    energy_charges DECIMAL(10,2) NOT NULL,
    fixed_charges DECIMAL(10,2) NOT NULL,
    meter_rent DECIMAL(10,2) NOT NULL,
    electricity_duty DECIMAL(10,2) NOT NULL,
    other_charges DECIMAL(10,2) DEFAULT 0.00,
    subsidy_amount DECIMAL(10,2) DEFAULT 0.00,
    late_fee DECIMAL(10,2) DEFAULT 0.00,
    total_amount DECIMAL(10,2) NOT NULL,
    previous_due DECIMAL(10,2) DEFAULT 0.00,
    net_payable DECIMAL(10,2) NOT NULL,
    amount_paid DECIMAL(10,2) DEFAULT 0.00,
    balance_amount DECIMAL(10,2) NOT NULL,
    bill_status ENUM('UNPAID', 'PARTIALLY_PAID', 'PAID', 'OVERDUE') DEFAULT 'UNPAID',
    pdf_path VARCHAR(255),
    qr_code_path VARCHAR(255),
    generated_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (reading_id) REFERENCES meter_readings(reading_id) ON DELETE RESTRICT,
    FOREIGN KEY (generated_by) REFERENCES users(user_id) ON DELETE SET NULL,
    UNIQUE KEY unique_bill (account_id, bill_month),
    INDEX idx_invoice_number (invoice_number),
    INDEX idx_account_month_bill (account_id, bill_month),
    INDEX idx_bill_status (bill_status),
    INDEX idx_due_date (due_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
--  PAYMENTS & COMPLAINTS
-- ------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS payments (
    payment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    payment_reference VARCHAR(50) UNIQUE NOT NULL,
    payment_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_amount DECIMAL(10,2) NOT NULL,
    convenience_fee DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    net_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    payment_mode ENUM('ONLINE', 'CASH', 'CHEQUE', 'UPI') NOT NULL,
    payment_channel VARCHAR(50),
    payment_status ENUM('SUCCESS', 'PENDING', 'FAILED') DEFAULT 'PENDING',
    transaction_id VARCHAR(100),
    upi_reference VARCHAR(100),
    cheque_number VARCHAR(50),
    cheque_date DATE,
    bank_name VARCHAR(100),
    remarks TEXT,
    receipt_url VARCHAR(255),
    processed_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE RESTRICT,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (processed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_payment_reference (payment_reference),
    INDEX idx_bill_id (bill_id),
    INDEX idx_account_id (account_id),
    INDEX idx_payment_date (payment_date),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS complaints (
    complaint_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    customer_id BIGINT NOT NULL,
    complaint_number VARCHAR(50) UNIQUE NOT NULL,
    complaint_type ENUM('BILLING', 'METER', 'SUPPLY', 'CONNECTION', 'OTHER') NOT NULL,
    subject VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') DEFAULT 'MEDIUM',
    status ENUM('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED') DEFAULT 'OPEN',
    assigned_to BIGINT,
    resolution TEXT,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (account_id) REFERENCES accounts(account_id) ON DELETE CASCADE,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE CASCADE,
    FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_complaint_number (complaint_number),
    INDEX idx_account_id (account_id),
    INDEX idx_customer_id (customer_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ------------------------------------------------------------------
--  SEED DATA
-- ------------------------------------------------------------------

INSERT INTO users (username, email, password, full_name, role)
SELECT 'admin', 'admin@vit.edu', '$2a$10$MmQhXIc5I5nxBI.0gQ.qFuK9/h2Uub4SmRyn8yXx6H3MwS26X5SZa', 'System Administrator', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');

INSERT INTO tariff_master (tariff_code, tariff_name, connection_type, description, fixed_charge, meter_rent, effective_from)
VALUES
    ('LT-I', 'Residential Low Tension', 'RESIDENTIAL', 'For domestic residential connections', 75.00, 25.00, '2025-01-01'),
    ('LT-II', 'Commercial Low Tension', 'COMMERCIAL', 'For commercial establishments', 150.00, 30.00, '2025-01-01'),
    ('LT-III', 'Industrial Low Tension', 'INDUSTRIAL', 'For small industrial units', 300.00, 50.00, '2025-01-01'),
    ('LT-IV', 'Agricultural Low Tension', 'AGRICULTURAL', 'For agricultural pumps', 50.00, 20.00, '2025-01-01')
ON DUPLICATE KEY UPDATE tariff_name = VALUES(tariff_name);

INSERT INTO tariff_slabs (tariff_id, slab_number, min_units, max_units, rate_per_unit)
VALUES
    (1, 1, 0, 100, 3.50),
    (1, 2, 101, 200, 5.00),
    (1, 3, 201, 300, 6.50),
    (1, 4, 301, NULL, 8.00),
    (2, 1, 0, 500, 7.00),
    (2, 2, 501, NULL, 9.00),
    (3, 1, 0, 1000, 6.00),
    (3, 2, 1001, NULL, 7.50),
    (4, 1, 0, NULL, 2.50)
ON DUPLICATE KEY UPDATE rate_per_unit = VALUES(rate_per_unit);

INSERT INTO additional_charges (charge_name, charge_type, charge_value, applicable_to)
VALUES
    ('Electricity Duty', 'PERCENTAGE', 16.00, 'ALL'),
    ('Fuel Adjustment Charge', 'PERCENTAGE', 5.00, 'ALL'),
    ('Wheeling Charges', 'PERCENTAGE', 3.00, 'LT-II,LT-III'),
    ('Convenience Fee ONLINE', 'PERCENTAGE', 1.50, 'ALL'),
    ('Convenience Fee UPI', 'PERCENTAGE', 0.25, 'ALL'),
    ('Convenience Fee CASH', 'FIXED', 0.00, 'ALL'),
    ('Convenience Fee CHEQUE', 'FIXED', 10.00, 'ALL')
ON DUPLICATE KEY UPDATE charge_value = VALUES(charge_value);

INSERT INTO subsidy_rules (tariff_code, connection_type, max_units, per_unit_subsidy, percentage_subsidy, fixed_subsidy, max_benefit, effective_from)
VALUES
    ('LT-I', 'RESIDENTIAL', 200, 1.50, 0.0000, 0.00, 400.00, '2025-01-01'),
    ('LT-IV', 'AGRICULTURAL', 300, 2.25, 0.0000, 0.00, 500.00, '2025-01-01')
ON DUPLICATE KEY UPDATE per_unit_subsidy = VALUES(per_unit_subsidy);

INSERT INTO late_fee_policies (connection_type, standard_due_days, grace_period_days, daily_rate_percentage, flat_fee, max_late_fee, effective_from)
VALUES
    ('RESIDENTIAL', 18, 5, 0.1000, 15.00, 300.00, '2025-01-01'),
    ('COMMERCIAL', 12, 3, 0.1500, 50.00, 1000.00, '2025-01-01'),
    ('INDUSTRIAL', 10, 2, 0.2000, 100.00, 1500.00, '2025-01-01'),
    ('AGRICULTURAL', 21, 7, 0.0500, 10.00, 200.00, '2025-01-01')
ON DUPLICATE KEY UPDATE flat_fee = VALUES(flat_fee);
