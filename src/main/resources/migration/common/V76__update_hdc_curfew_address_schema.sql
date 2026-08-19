ALTER TABLE hdc_curfew_address
    RENAME COLUMN address_line1 TO first_line;
ALTER TABLE hdc_curfew_address
    RENAME COLUMN address_line2 TO second_line;


ALTER TABLE hdc_curfew_address
    ADD COLUMN reference VARCHAR(255),
    ADD COLUMN uprn VARCHAR(255),
    ADD COLUMN source VARCHAR(50),
    ADD COLUMN accommodation_type VARCHAR(50),
    ADD COLUMN post_release_residential_checks_completed BOOLEAN,
    ADD COLUMN post_release_residential_checks_not_completed_reason VARCHAR(1000),
    ADD COLUMN created_timestamp TIMESTAMP WITH TIME ZONE,
    ADD COLUMN last_updated_timestamp TIMESTAMP WITH TIME ZONE;

UPDATE hdc_curfew_address
SET reference = gen_random_uuid()::text
WHERE reference IS NULL;

UPDATE hdc_curfew_address
SET source = 'MANUAL_MIGRATED'
WHERE source IS NULL;

UPDATE hdc_curfew_address
SET created_timestamp = CURRENT_TIMESTAMP
WHERE created_timestamp IS NULL;

UPDATE hdc_curfew_address
SET last_updated_timestamp = CURRENT_TIMESTAMP
WHERE last_updated_timestamp IS NULL;

ALTER TABLE hdc_curfew_address
    ALTER COLUMN first_line SET NOT NULL,
    ALTER COLUMN reference SET NOT NULL,
    ALTER COLUMN source SET NOT NULL,
    ALTER COLUMN created_timestamp SET NOT NULL,
    ALTER COLUMN last_updated_timestamp SET NOT NULL;

ALTER TABLE hdc_curfew_address
    ADD CONSTRAINT uk_hdc_curfew_address_reference UNIQUE (reference);

ALTER TABLE hdc_curfew_address
    ALTER COLUMN source SET DEFAULT 'MANUAL',
    ALTER COLUMN created_timestamp SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN last_updated_timestamp SET DEFAULT CURRENT_TIMESTAMP;
