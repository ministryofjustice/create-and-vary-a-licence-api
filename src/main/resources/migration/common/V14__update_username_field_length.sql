ALTER TABLE licence ALTER COLUMN created_by_username TYPE varchar(100);
ALTER TABLE licence ALTER COLUMN updated_by_username TYPE varchar(100);
ALTER TABLE licence ALTER COLUMN approved_by_username TYPE varchar(100);
ALTER TABLE licence_history ALTER COLUMN action_username TYPE varchar(100);
