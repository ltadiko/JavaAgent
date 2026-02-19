-- ╔═══════════════════════════════════════════════════════════════════════════╗
-- ║  V8 — Row-Level Security policies for tenant isolation                    ║
-- ╚═══════════════════════════════════════════════════════════════════════════╝

-- Enable RLS on all application tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE cv_details ENABLE ROW LEVEL SECURITY;
ALTER TABLE cv_embeddings ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_embeddings ENABLE ROW LEVEL SECURITY;
ALTER TABLE saved_jobs ENABLE ROW LEVEL SECURITY;
ALTER TABLE job_source_configs ENABLE ROW LEVEL SECURITY;
ALTER TABLE motivation_letters ENABLE ROW LEVEL SECURITY;
ALTER TABLE motivation_letter_history ENABLE ROW LEVEL SECURITY;
ALTER TABLE applications ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE application_notes ENABLE ROW LEVEL SECURITY;

-- Create policies — each enforces: tenant_id = current_setting('app.current_tenant')
CREATE POLICY tenant_isolation_users ON users
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_profiles ON user_profiles
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_cv ON cv_details
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_cv_embed ON cv_embeddings
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_jobs ON job_listings
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_job_embed ON job_embeddings
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_saved ON saved_jobs
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_sources ON job_source_configs
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_letters ON motivation_letters
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_letter_hist ON motivation_letter_history
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_apps ON applications
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_app_events ON application_events
    USING (tenant_id = current_setting('app.current_tenant')::uuid);
CREATE POLICY tenant_isolation_app_notes ON application_notes
    USING (tenant_id = current_setting('app.current_tenant')::uuid);

-- Note: The 'jobagent' database user needs to NOT be a superuser for RLS to take effect.
-- In production, create a dedicated app user:
--   CREATE ROLE jobagent_app LOGIN PASSWORD '...' NOSUPERUSER;
--   GRANT ALL ON ALL TABLES IN SCHEMA public TO jobagent_app;
