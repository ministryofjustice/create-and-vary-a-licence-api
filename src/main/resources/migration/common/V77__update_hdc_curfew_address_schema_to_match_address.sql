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

UPDATE hdc_curfew_address
SET accommodation_type = UPPER(accommodation_type)
WHERE accommodation_type IS NOT NULL;

DO $$
    BEGIN
        IF EXISTS (SELECT 1 FROM hdc_curfew_address WHERE first_line IS NULL) THEN
            RAISE EXCEPTION 'first_line contains NULL values';
        END IF;

        IF EXISTS (SELECT 1 FROM hdc_curfew_address WHERE town_or_city IS NULL) THEN
            RAISE EXCEPTION 'town_or_city contains NULL values';
        END IF;

        IF EXISTS (SELECT 1 FROM hdc_curfew_address WHERE postcode IS NULL) THEN
            RAISE EXCEPTION 'postcode contains NULL values';
        END IF;

        IF EXISTS (SELECT 1 FROM hdc_curfew_address WHERE reference IS NULL) THEN
            RAISE EXCEPTION 'reference contains NULL values';
        END IF;

        IF EXISTS (SELECT 1 FROM hdc_curfew_address WHERE accommodation_type IS NULL) THEN
            RAISE EXCEPTION 'accommodation_type contains NULL values';
        END IF;
    END$$;

UPDATE hdc_curfew_address
SET created_timestamp = CURRENT_TIMESTAMP
WHERE created_timestamp IS NULL;

UPDATE hdc_curfew_address
SET last_updated_timestamp = CURRENT_TIMESTAMP
WHERE last_updated_timestamp IS NULL;

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

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'uk_hdc_curfew_address_reference'
        ) THEN
            ALTER TABLE hdc_curfew_address
                ADD CONSTRAINT uk_hdc_curfew_address_reference UNIQUE (reference);
        END IF;
    END$$;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'chk_hdc_curfew_address_source_valid'
        ) THEN
            ALTER TABLE hdc_curfew_address
                ADD CONSTRAINT chk_hdc_curfew_address_source_valid
                    CHECK (source IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED'));
        END IF;
    END$$;

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'chk_hdc_curfew_address_accommodation_type_valid'
        ) THEN
            ALTER TABLE hdc_curfew_address
                ADD CONSTRAINT chk_hdc_curfew_address_accommodation_type_valid
                    CHECK (accommodation_type IN ('CAS', 'RESIDENTIAL'));
        END IF;
    END$$;
