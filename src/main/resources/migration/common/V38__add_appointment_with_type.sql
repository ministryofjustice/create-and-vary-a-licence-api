ALTER TABLE licence
    ADD COLUMN appointment_person_type VARCHAR(100);
UPDATE licence
SET appointment_person_type = 'SPECIFIC_PERSON'
WHERE appointment_person IS NOT NULL;
