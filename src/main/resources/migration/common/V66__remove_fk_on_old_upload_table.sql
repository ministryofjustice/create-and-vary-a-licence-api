BEGIN;

	-- This FK has already been removed in prod manually, hence IF EXISTS
	ALTER TABLE additional_condition_upload_summary DROP CONSTRAINT IF EXISTS additional_condition_upload_summar_additional_condition_id_fkey;

COMMIT;
