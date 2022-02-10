-------------------------------------------------------
----- Create new community_offender_manager table -----
-------------------------------------------------------
CREATE TABLE community_offender_manager
(
    id SERIAL NOT NULL PRIMARY KEY,
    staff_identifier INTEGER NOT NULL UNIQUE,
    username VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(200),
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-----------------------------------------------------------------------
----- Create new community_offender_manager_licence mapping table -----
-----------------------------------------------------------------------
CREATE TABLE community_offender_manager_licence_mailing_list
(
    id SERIAL NOT NULL PRIMARY KEY,
    community_offender_manager_id INTEGER NOT NULL REFERENCES community_offender_manager(id),
    licence_id INTEGER NOT NULL REFERENCES licence(id),
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

---------------------------------------------------------------------
--- Drop redundant columns in licence table -----
---------------------------------------------------------------------
ALTER TABLE licence DROP COLUMN com_first_name;
ALTER TABLE licence DROP COLUMN com_last_name;
ALTER TABLE licence DROP COLUMN com_username;
ALTER TABLE licence DROP COLUMN com_staff_id;
ALTER TABLE licence DROP COLUMN com_email;
ALTER TABLE licence DROP COLUMN com_telephone;

----------------------------------------------------------------------------------
----- Add new columns in licence table with default values for existing rows -----
----------------------------------------------------------------------------------
ALTER TABLE licence ADD COLUMN responsible_com_id INTEGER NOT NULL DEFAULT -1 REFERENCES community_offender_manager(id);
ALTER TABLE licence ADD COLUMN created_by_com_id INTEGER NOT NULL DEFAULT -1 REFERENCES community_offender_manager(id);
ALTER TABLE licence ADD COLUMN submitted_by_com_id INTEGER REFERENCES community_offender_manager(id);

----------------------------------------------------------------------------------
----- Remove default value constraint -----
----------------------------------------------------------------------------------
ALTER TABLE licence ALTER COLUMN responsible_com_id DROP DEFAULT;
ALTER TABLE licence ALTER COLUMN created_by_com_id DROP DEFAULT;
