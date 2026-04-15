CREATE TABLE hdc_migration_condition_meta_data (
   id BIGSERIAL PRIMARY KEY,
   condition_id BIGINT NOT NULL,
   hdc_condition_code VARCHAR(50) NOT NULL,
   hdc_condition_version INTEGER NULL
);

CREATE INDEX idx_hdc_migration_condition_meta_data_condition_id ON hdc_migration_condition_meta_data (condition_id);

-- add new type of source
ALTER TABLE address DROP CONSTRAINT chk_address_source_valid;
ALTER TABLE address
	ADD CONSTRAINT chk_address_source_valid
		CHECK (source IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED'));
