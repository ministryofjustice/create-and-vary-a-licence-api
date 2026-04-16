CREATE TABLE hdc_migration_condition_meta_data (
   id BIGSERIAL PRIMARY KEY,
   licence_id BIGINT NOT NULL,
   condition_id BIGINT NOT NULL,
   hdc_condition_code VARCHAR(50) NOT NULL,
   hdc_condition_version INTEGER NULL
);

CREATE INDEX idx_hdc_migration_condition_meta_data_condition_id ON hdc_migration_condition_meta_data (condition_id);


CREATE TABLE hdc_migration_meta_data (
   id BIGSERIAL PRIMARY KEY,
   licence_id BIGINT NOT NULL,
   hdcLicence_id BIGINT NOT NULL,
   licence_version INTEGER NOT NULL,
   vary_version INTEGER NOT NULL
);

CREATE INDEX idx_hdc_migration_meta_data_id ON hdc_migration_meta_data (licence_id);


-- Add new type of source
ALTER TABLE address DROP CONSTRAINT chk_address_source_valid;
ALTER TABLE address
	ADD CONSTRAINT chk_address_source_valid
		CHECK (source IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED'));
