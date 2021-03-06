CREATE TABLE community_offender_manager
(
    id SERIAL NOT NULL PRIMARY KEY,
    staff_identifier INTEGER NOT NULL UNIQUE,
    username VARCHAR(40) NOT NULL UNIQUE,
    email VARCHAR(200),
    first_name VARCHAR(60),
    last_name VARCHAR(60),
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
