ALTER TABLE hdc_curfew_address
    ALTER COLUMN reference TYPE TEXT,
    ALTER COLUMN uprn TYPE TEXT,
    ALTER COLUMN source TYPE TEXT,
    ALTER COLUMN accommodation_type TYPE TEXT,
    ALTER COLUMN post_release_residential_checks_not_completed_reason TYPE TEXT,
    ALTER COLUMN first_line TYPE TEXT,
    ALTER COLUMN second_line TYPE TEXT,
    ALTER COLUMN town_or_city TYPE TEXT,
    ALTER COLUMN county TYPE TEXT,
    ALTER COLUMN postcode TYPE TEXT;

UPDATE hdc_curfew_address
SET source = 'MANUAL'
WHERE source IS NULL
   OR source NOT IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED');

ALTER TABLE hdc_curfew_address
    ALTER COLUMN source SET DEFAULT 'MANUAL',
    ALTER COLUMN created_timestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN last_updated_timestamp SET DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE hdc_curfew_address
    ALTER COLUMN reference SET NOT NULL,
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN created_timestamp SET NOT NULL,
    ALTER COLUMN last_updated_timestamp SET NOT NULL,
    ALTER COLUMN first_line SET NOT NULL,
    ALTER COLUMN town_or_city SET NOT NULL,
    ALTER COLUMN postcode SET NOT NULL,
    ALTER COLUMN accommodation_type SET NOT NULL;

ALTER TABLE hdc_curfew_address
    ADD CONSTRAINT chk_hdc_curfew_address_source_valid
        CHECK (source IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED'));

ALTER TABLE hdc_curfew_address
    ADD CONSTRAINT chk_hdc_curfew_address_accommodation_type_valid
        CHECK (accommodation_type IN ('CAS', 'RESIDENTIAL'));
