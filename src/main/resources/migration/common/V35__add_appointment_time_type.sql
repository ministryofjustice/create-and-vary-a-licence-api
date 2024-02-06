ALTER TABLE licence
    ADD COLUMN appointment_time_type VARCHAR(100);
UPDATE licence
SET appointment_time_type = 'SPECIFIC_DATE_TIME'
WHERE appointment_time IS NOT NULL;
