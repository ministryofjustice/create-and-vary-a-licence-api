CREATE TABLE licence_type (
  type_code varchar(20) NOT NULL constraint licence_type_pk PRIMARY KEY,
  description varchar(40)
);

CREATE TABLE licence_status (
  status_code varchar(40) NOT NULL constraint licence_status_pk PRIMARY KEY,
  description varchar(40)
);

CREATE TABLE licence (
  id serial NOT NULL constraint licence_pk PRIMARY KEY,
  type_code varchar(20) references licence_type(type_code),
  version varchar(10),  -- records the version number held in configuration at the time of creation
  status_code varchar(40) references licence_status(status_code),
  noms_id varchar(7),
  booking_no varchar(8),
  booking_id integer,
  crn varchar(7),
  pnc varchar(12),
  cro varchar(15),
  prison_code varchar(3),
  prison_description varchar(40),
  prison_telephone varchar(20),
  forename varchar(60),
  middle_names varchar(60),
  surname varchar(60),
  date_of_birth date,
  conditional_release_date date,
  actual_release_date date,
  sentence_start_date date,
  sentence_end_date date,
  licence_start_date date,
  licence_expiry_date date,
  topup_supervision_start_date date,
  topup_supervision_expiry_date date,
  probation_area_code varchar(10),
  probation_ldu_code varchar(20),
  appointment_person varchar(60),
  appointment_time timestamp with time zone,
  appointment_address varchar(240),
  appointment_contact varchar(20),
  approved_date timestamp with time zone,
  approved_by_username varchar(100),
  approved_by_name varchar(100),
  superseded_date timestamp with time zone,
  responsible_com_id INTEGER NOT NULL REFERENCES community_offender_manager(id),
  created_by_com_id INTEGER NOT NULL REFERENCES community_offender_manager(id),
  submitted_by_com_id INTEGER REFERENCES community_offender_manager(id),
  date_created timestamp with time zone default CURRENT_TIMESTAMP,
  created_by_username varchar(100),
  date_last_updated timestamp with time zone default CURRENT_TIMESTAMP,
  updated_by_username varchar(100)
);

CREATE INDEX idx_licence_noms_id ON licence(noms_id);
CREATE INDEX idx_licence_crn ON licence(crn);

CREATE TABLE standard_condition (
  id serial NOT NULL constraint standard_condition_pk PRIMARY KEY,
  licence_id integer NOT NULL references licence(id),
  condition_code varchar(50) NOT NULL,
  condition_sequence integer NOT NULL,
  condition_text text NOT NULL,
  condition_type varchar(20) NOT NULL
);

CREATE INDEX idx_standard_condition_licence_id ON standard_condition(licence_id);

CREATE TABLE additional_condition (
  id serial NOT NULL constraint additional_condition_pk PRIMARY KEY,
  licence_id integer references licence(id),
  condition_category text NOT NULL,
  condition_code varchar(50) NOT NULL,  -- copied from the current licence configuration for additional conditions
  condition_sequence integer, -- copied from the current configuration for additional conditions
  condition_text text NOT NULL, -- copied from the current configuration for additional conditions
  condition_type varchar(20) NOT NULL
);

CREATE INDEX idx_additional_condition_licence_id ON additional_condition(licence_id);

-- There can be more than one data item for a single additional term
CREATE TABLE additional_condition_data (
  id serial NOT NULL constraint additional_condition_data_pk PRIMARY KEY,
  additional_condition_id integer references additional_condition(id),
  data_sequence integer,  -- the sequence of the data for this additional condition - starting at 1
  data_field varchar(60), -- copied from configuration for this additional condition data
  data_value text -- the value collected from the user
);

CREATE INDEX idx_additional_condition_data_id ON additional_condition_data(additional_condition_id);

CREATE TABLE bespoke_condition (
  id serial NOT NULL constraint bespoke_condition_pk PRIMARY KEY,
  licence_id integer references licence(id),
  condition_sequence integer,
  condition_text text NOT NULL
);

CREATE INDEX idx_bespoke_condition_licence_id ON bespoke_condition(licence_id);

CREATE TABLE licence_history (
  id serial NOT NULL constraint licence_history_pk PRIMARY KEY,
  licence_id integer references licence(id),
  status_code varchar(20) references licence_status(status_code),
  action_time timestamp with time zone default CURRENT_TIMESTAMP,
  action_description varchar(80) NOT NULL,
  action_username varchar(100)
);

CREATE INDEX idx_licence_history_id ON licence_history(licence_id);

-- A record here whenever a licence document is approved, created and stored in S3
CREATE TABLE licence_document (
  id serial NOT NULL constraint licence_document_pk PRIMARY KEY,
  licence_id integer references licence(id),
  created_time timestamp with time zone default CURRENT_TIMESTAMP,
  s3_location varchar(200),
  document_name varchar(100),
  expiry_tme timestamp with time zone default CURRENT_TIMESTAMP,
  meta_data varchar(100)
);

CREATE INDEX idx_licence_document_id ON licence_document(licence_id);

-- Table to contain a summary view of files uploaded for an additional condition
CREATE TABLE additional_condition_upload_summary (
    id serial NOT NULL constraint additional_condition_upload_summary_pk PRIMARY KEY,
    additional_condition_id integer references additional_condition(id),
    filename varchar(200), -- the original name of the file uploaded
    file_type varchar(60), -- mime type - only application/pdf
    file_size integer, -- in bytes
    uploaded_time timestamp with time zone default CURRENT_TIMESTAMP,
    description text, -- the extracted text describing the image
    thumbnail_image bytea, -- the extracted, scaled, thumbnail image data PNG
    upload_detail_id integer -- ID of the related detail row (intentionally no FK link)
);

CREATE INDEX idx_upload_summary_condition_id ON additional_condition_upload_summary(additional_condition_id);

-- Table to hold the detail data for uploads - intentionally not linked by referential integrity (manually managed)
CREATE TABLE additional_condition_upload_detail (
    id serial NOT NULL constraint additional_condition_upload_detail_pk PRIMARY KEY,
    licence_id integer,
    additional_condition_id integer,
    original_data bytea, -- the originally uploaded file content (PDF)
    full_size_image bytea -- the extracted, scaled, full-size image data PNG
);

CREATE INDEX idx_upload_detail_licence_id ON additional_condition_upload_detail(licence_id);
CREATE INDEX idx_upload_detail_condition_id ON additional_condition_upload_detail(additional_condition_id);

CREATE TABLE community_offender_manager_licence_mailing_list
(
    id SERIAL NOT NULL PRIMARY KEY,
    community_offender_manager_id INTEGER NOT NULL REFERENCES community_offender_manager(id),
    licence_id INTEGER NOT NULL REFERENCES licence(id),
    created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
