ALTER TABLE hdc_postponement
    RENAME TO hdc_application;

ALTER TABLE hdc_application
    ADD COLUMN proposed_accommodation_type TEXT;

ALTER TABLE hdc_application
    ALTER COLUMN is_postponed SET DEFAULT false;

ALTER INDEX ux_hdc_postponement_licence_id
    RENAME TO ux_hdc_application_licence_id;

ALTER TABLE hdc_curfew_address
    DROP CONSTRAINT chk_hdc_curfew_address_source_valid,
    ADD CONSTRAINT chk_hdc_curfew_address_source_valid
        CHECK (source IN ('MANUAL', 'OS_PLACES', 'MANUAL_MIGRATED', 'HDC_CREATION'));