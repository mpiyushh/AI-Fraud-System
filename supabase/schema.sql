-- =============================================================================
-- SENTINEL AI FRAUD DETECTION SYSTEM
-- Supabase / PostgreSQL Production Schema Definition with RLS Policies
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. USER PROFILES TABLE (Feature store & baseline behavioral stats)
CREATE TABLE IF NOT EXISTS user_profiles (
    user_id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    primary_location VARCHAR(255),
    trusted_device VARCHAR(255),
    avg_amount DOUBLE PRECISION DEFAULT 50.0,
    std_dev DOUBLE PRECISION DEFAULT 20.0,
    tx_count INTEGER DEFAULT 1,
    chargeback_count INTEGER DEFAULT 0,
    is_blacklisted BOOLEAN DEFAULT FALSE,
    last_seen_timestamp BIGINT,
    last_seen_location VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. TRANSACTIONS TABLE (Ingested events + fraud analysis verdicts)
CREATE TABLE IF NOT EXISTS transactions (
    tx_id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    card_masked VARCHAR(64) NOT NULL,
    bank VARCHAR(64) DEFAULT 'Chase',
    amount DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    tx_timestamp BIGINT NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    location VARCHAR(255) NOT NULL,
    device_hash VARCHAR(255) NOT NULL,
    is_vpn_tor BOOLEAN NOT NULL DEFAULT FALSE,
    risk_score INTEGER,
    verdict VARCHAR(64),
    dominant_threat VARCHAR(255),
    analyst_status VARCHAR(64) DEFAULT 'PENDING',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 3. AUDIT LOGS TABLE (Manual analyst overrides & security actions)
CREATE TABLE IF NOT EXISTS audit_logs (
    log_id BIGSERIAL PRIMARY KEY,
    tx_id VARCHAR(64) NOT NULL,
    analyst_name VARCHAR(255) NOT NULL,
    action_taken VARCHAR(255) NOT NULL,
    timestamp BIGINT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 4. SYSTEM CONFIGURATIONS TABLE (Admin threshold & engine configuration)
CREATE TABLE IF NOT EXISTS system_configurations (
    config_key VARCHAR(64) PRIMARY KEY DEFAULT 'DEFAULT',
    block_threshold INTEGER DEFAULT 80,
    challenge_threshold INTEGER DEFAULT 55,
    velocity_limit INTEGER DEFAULT 3,
    max_amount DOUBLE PRECISION DEFAULT 10000.0,
    tor_block BOOLEAN DEFAULT TRUE,
    model_version VARCHAR(128) DEFAULT 'v4.8-Neural-Ensemble',
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for high-speed queries
CREATE INDEX IF NOT EXISTS idx_transactions_timestamp ON transactions (tx_timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions (user_id);
CREATE INDEX IF NOT EXISTS idx_transactions_verdict ON transactions (verdict);
CREATE INDEX IF NOT EXISTS idx_transactions_analyst_status ON transactions (analyst_status);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tx_id ON audit_logs (tx_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp DESC);

-- =============================================================================
-- ROW LEVEL SECURITY (RLS) POLICIES
-- =============================================================================

-- Enable Row Level Security on all tables
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE system_configurations ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Clean up any existing policies
DROP POLICY IF EXISTS "Allow read user_profiles" ON user_profiles;
DROP POLICY IF EXISTS "Allow insert user_profiles" ON user_profiles;
DROP POLICY IF EXISTS "Allow update user_profiles" ON user_profiles;
DROP POLICY IF EXISTS "Allow delete user_profiles" ON user_profiles;

DROP POLICY IF EXISTS "Allow read transactions" ON transactions;
DROP POLICY IF EXISTS "Allow insert transactions" ON transactions;
DROP POLICY IF EXISTS "Allow update transactions" ON transactions;
DROP POLICY IF EXISTS "Allow delete transactions" ON transactions;

DROP POLICY IF EXISTS "Allow read system_configurations" ON system_configurations;
DROP POLICY IF EXISTS "Allow insert system_configurations" ON system_configurations;
DROP POLICY IF EXISTS "Allow update system_configurations" ON system_configurations;

DROP POLICY IF EXISTS "Allow read audit_logs" ON audit_logs;
DROP POLICY IF EXISTS "Allow insert audit_logs" ON audit_logs;

-- Policies for user_profiles
CREATE POLICY "Allow read user_profiles" ON user_profiles
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert user_profiles" ON user_profiles
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update user_profiles" ON user_profiles
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Allow delete user_profiles" ON user_profiles
    FOR DELETE TO anon, authenticated
    USING (true);

-- Policies for transactions
CREATE POLICY "Allow read transactions" ON transactions
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert transactions" ON transactions
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update transactions" ON transactions
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Allow delete transactions" ON transactions
    FOR DELETE TO anon, authenticated
    USING (true);

-- Policies for system_configurations
CREATE POLICY "Allow read system_configurations" ON system_configurations
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert system_configurations" ON system_configurations
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update system_configurations" ON system_configurations
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- Policies for audit_logs (append-only + read)
CREATE POLICY "Allow read audit_logs" ON audit_logs
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert audit_logs" ON audit_logs
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

-- =============================================================================
-- SEED INITIAL DATA
-- =============================================================================

-- Seed Initial User Profiles
INSERT INTO user_profiles (user_id, full_name, primary_location, trusted_device, avg_amount, std_dev, tx_count, chargeback_count, is_blacklisted, last_seen_timestamp, last_seen_location)
VALUES
    ('USR-101', 'Alexandre V.', 'London, UK', 'DEV_MACBOOK_PRO_9', 420.0, 80.0, 14, 0, FALSE, 1600000000000, 'London, UK'),
    ('USR-102', 'Sarah Jenkins', 'Austin, US', 'DEV_IPHONE_15_PRO', 45.0, 15.0, 48, 0, FALSE, 1600000000000, 'Austin, US'),
    ('USR-103', 'Liam O''Connor', 'Dublin, IE', 'DEV_IPAD_AIR_2', 120.0, 40.0, 8, 0, FALSE, 1600000000000, 'Dublin, IE'),
    ('USR-BAD', 'Botnet Cluster Alpha', 'Unknown', 'DEV_SPOOFED_EMULATOR_01', 5000.0, 200.0, 0, 3, TRUE, 1600000000000, 'Tor Exit Node')
ON CONFLICT (user_id) DO UPDATE SET
    full_name = EXCLUDED.full_name,
    primary_location = EXCLUDED.primary_location,
    trusted_device = EXCLUDED.trusted_device,
    avg_amount = EXCLUDED.avg_amount,
    std_dev = EXCLUDED.std_dev,
    tx_count = EXCLUDED.tx_count,
    chargeback_count = EXCLUDED.chargeback_count,
    is_blacklisted = EXCLUDED.is_blacklisted,
    last_seen_timestamp = EXCLUDED.last_seen_timestamp,
    last_seen_location = EXCLUDED.last_seen_location;

-- Seed Initial System Configuration
INSERT INTO system_configurations (config_key, block_threshold, challenge_threshold, velocity_limit, max_amount, tor_block, model_version)
VALUES ('DEFAULT', 80, 55, 3, 10000.0, TRUE, 'v4.8-Neural-Ensemble')
ON CONFLICT (config_key) DO UPDATE SET
    block_threshold = EXCLUDED.block_threshold,
    challenge_threshold = EXCLUDED.challenge_threshold,
    velocity_limit = EXCLUDED.velocity_limit,
    max_amount = EXCLUDED.max_amount,
    tor_block = EXCLUDED.tor_block,
    model_version = EXCLUDED.model_version;
