ALTER TABLE audit_event ADD COLUMN changes jsonb DEFAULT '{}'::jsonb;
