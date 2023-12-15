CREATE TABLE community_offender_manager
(
    id SERIAL NOT NULL constraint community_offender_manager_key PRIMARY KEY,
    staff_identifier INTEGER NOT NULL CONSTRAINT community_offender_manager_staff_identifier_key UNIQUE,
    username VARCHAR(40) NOT NULL CONSTRAINT community_offender_manager_username_key UNIQUE,
    email VARCHAR(200),
    first_name VARCHAR(60),
    last_name VARCHAR(60),
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
