ALTER TABLE licence
    DROP COLUMN licence_activated_date;

ALTER TABLE licence
    ADD COLUMN licence_activated_date TIMESTAMP WITH TIME ZONE;