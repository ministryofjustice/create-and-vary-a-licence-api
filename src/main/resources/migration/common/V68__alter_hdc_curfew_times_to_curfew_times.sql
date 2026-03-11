ALTER TABLE hdc_curfew_times RENAME TO curfew_times;

-- to remove licence_id column
ALTER TABLE curfew_times DROP CONSTRAINT IF EXISTS hdc_curfew_times_licence_id_fkey;
DROP INDEX IF EXISTS idx_hdc_curfew_times_licence_id;
ALTER TABLE curfew_times DROP COLUMN IF EXISTS licence_id;
