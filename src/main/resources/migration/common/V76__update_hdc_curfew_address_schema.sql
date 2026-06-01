ALTER TABLE hdc_curfew_address
    RENAME COLUMN address_line1 TO first_line;

ALTER TABLE hdc_curfew_address
    RENAME COLUMN address_line2 TO second_line;

ALTER TABLE hdc_curfew_address
    ADD COLUMN reference VARCHAR(255) UNIQUE;

UPDATE hdc_curfew_address
    SET reference = gen_random_uuid()::text
WHERE reference IS NULL;

ALTER TABLE hdc_curfew_address
    ADD COLUMN uprn VARCHAR(255);

ALTER TABLE hdc_curfew_address
    ADD COLUMN source VARCHAR(50) DEFAULT 'MANUAL_MIGRATED';

ALTER TABLE hdc_curfew_address
    ADD COLUMN accommodation_type VARCHAR(50);

ALTER TABLE hdc_curfew_address
    ADD COLUMN residential_checks_completed BOOLEAN;

ALTER TABLE hdc_curfew_address
    ADD COLUMN residential_checks_not_completed_reason VARCHAR(1000);

ALTER TABLE hdc_curfew_address
    ALTER COLUMN first_line SET NOT NULL;

ALTER TABLE hdc_curfew_address
    ALTER COLUMN reference SET NOT NULL;

ALTER TABLE hdc_curfew_address
    ALTER COLUMN source SET NOT NULL;
