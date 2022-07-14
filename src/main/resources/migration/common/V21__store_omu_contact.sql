CREATE TABLE omu_contact
(
    id serial NOT NULL constraint omu_contact_pk PRIMARY KEY,
    prison_code         VARCHAR(255),
    email               VARCHAR(255),
    date_created        TIMESTAMP WITHOUT TIME ZONE,
    date_last_updated   TIMESTAMP WITHOUT TIME ZONE
);
