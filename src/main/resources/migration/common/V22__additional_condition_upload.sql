CREATE TABLE additional_condition_upload
(
    id                      serial NOT NULL
        constraint additional_condition_upload_contact_pk PRIMARY KEY,
    key                     VARCHAR(255) UNIQUE,
    category                VARCHAR(255),
    url                     text,
    mine_type               varchar(255),
    licence_id              serial NOT NULL references licence (id),
    additional_condition_id serial NOT NULL references additional_condition (id)
)