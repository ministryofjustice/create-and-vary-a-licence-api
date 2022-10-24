ALTER TABLE additional_condition
    ADD COLUMN condition_version VARCHAR(6);

update additional_condition
set condition_version = (select licence.version from licence where licence.id = additional_condition.licence_id)
where exists(select * from licence where licence.id = additional_condition.licence_id);

ALTER TABLE additional_condition
    ALTER COLUMN condition_version SET NOT NULL;
