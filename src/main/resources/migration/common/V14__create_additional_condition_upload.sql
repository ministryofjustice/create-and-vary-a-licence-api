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