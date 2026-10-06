-- =============================================================================
-- SENTINEL AI FRAUD DETECTION SYSTEM
-- Row Level Security (RLS) Policies for Supabase
-- Run this script in the Supabase Dashboard -> SQL Editor
-- =============================================================================

-- 1. Enable Row Level Security on all application tables
ALTER TABLE public.user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.transactions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.system_configurations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.audit_logs ENABLE ROW LEVEL SECURITY;

-- 2. Drop any existing duplicate policies
DROP POLICY IF EXISTS "Allow read user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow insert user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow update user_profiles" ON public.user_profiles;
DROP POLICY IF EXISTS "Allow delete user_profiles" ON public.user_profiles;

DROP POLICY IF EXISTS "Allow read transactions" ON public.transactions;
DROP POLICY IF EXISTS "Allow insert transactions" ON public.transactions;
DROP POLICY IF EXISTS "Allow update transactions" ON public.transactions;
DROP POLICY IF EXISTS "Allow delete transactions" ON public.transactions;

DROP POLICY IF EXISTS "Allow read system_configurations" ON public.system_configurations;
DROP POLICY IF EXISTS "Allow insert system_configurations" ON public.system_configurations;
DROP POLICY IF EXISTS "Allow update system_configurations" ON public.system_configurations;

DROP POLICY IF EXISTS "Allow read audit_logs" ON public.audit_logs;
DROP POLICY IF EXISTS "Allow insert audit_logs" ON public.audit_logs;

-- 3. Dedicated Granular Policies for user_profiles
CREATE POLICY "Allow read user_profiles" ON public.user_profiles
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert user_profiles" ON public.user_profiles
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update user_profiles" ON public.user_profiles
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Allow delete user_profiles" ON public.user_profiles
    FOR DELETE TO anon, authenticated
    USING (true);

-- 4. Dedicated Granular Policies for transactions
CREATE POLICY "Allow read transactions" ON public.transactions
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert transactions" ON public.transactions
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update transactions" ON public.transactions
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

CREATE POLICY "Allow delete transactions" ON public.transactions
    FOR DELETE TO anon, authenticated
    USING (true);

-- 5. Dedicated Granular Policies for system_configurations
CREATE POLICY "Allow read system_configurations" ON public.system_configurations
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert system_configurations" ON public.system_configurations
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);

CREATE POLICY "Allow update system_configurations" ON public.system_configurations
    FOR UPDATE TO anon, authenticated
    USING (true)
    WITH CHECK (true);

-- 6. Dedicated Granular Policies for audit_logs (append-only & read)
CREATE POLICY "Allow read audit_logs" ON public.audit_logs
    FOR SELECT TO anon, authenticated
    USING (true);

CREATE POLICY "Allow insert audit_logs" ON public.audit_logs
    FOR INSERT TO anon, authenticated
    WITH CHECK (true);
