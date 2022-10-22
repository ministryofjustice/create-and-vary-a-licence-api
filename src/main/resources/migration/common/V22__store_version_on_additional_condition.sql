ALTER TABLE additional_condition ADD COLUMN condition_version VARCHAR(6);

UPDATE additional_condition ac
SET ac.condition_version = version
    FROM licence l
WHERE ac.licence_id = l.id;

ALTER TABLE additional_condition ALTER COLUMN condition_version SET NOT NULL;
