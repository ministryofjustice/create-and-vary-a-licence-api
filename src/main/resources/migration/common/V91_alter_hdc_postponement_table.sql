ALTER TABLE hdc_postponement
    RENAME TO hdc_application;

ALTER TABLE hdc_application
    ADD COLUMN proposed_accommodation_type TEXT;

ALTER INDEX ux_hdc_postponement_licence_id
    RENAME TO ux_hdc_application_licence_id;