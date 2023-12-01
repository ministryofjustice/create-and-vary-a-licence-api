ALTER TABLE licence
    ADD COLUMN kind varchar(20);

UPDATE licence SET kind = CASE WHEN variation_of_id is null THEN 'CRD' ELSE 'VARIATION' END;

ALTER TABLE licence ALTER COLUMN kind SET NOT NULL;
