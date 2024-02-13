ALTER TABLE licence
    ADD COLUMN appointment_with_type VARCHAR(100);
UPDATE licence
SET appointment_with_type = 'SOMEONE_ELSE'
WHERE appointment_person IS NOT NULL;
