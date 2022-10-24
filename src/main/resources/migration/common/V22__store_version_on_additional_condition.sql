ALTER TABLE additional_condition
    ADD COLUMN condition_version VARCHAR(6);

update additional_condition as ac
set ac.condition_version = (select l.version from licence l where l.id = ac.licence_id)
where exists(select * from licence l where l.id = ac.licence_id);

ALTER TABLE additional_condition
    ALTER COLUMN condition_version SET NOT NULL;
