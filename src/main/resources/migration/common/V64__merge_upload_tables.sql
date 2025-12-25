BEGIN;

-- Create new table
CREATE TABLE public.additional_condition_upload (
													id                      serial4 NOT NULL,
	-- shared
													additional_condition_id int4 NULL,
													licence_id              int4 NULL,
	-- summary fields
													filename                varchar(200) NULL,
													file_type               varchar(60) NULL,
													file_size               int4 NULL,
													uploaded_time           timestamptz DEFAULT CURRENT_TIMESTAMP NULL,
													description             text NULL,
	-- Removed thumbnail_image         bytea NULL,
													thumbnail_image_ds_uuid varchar(36) NULL,
													image_type              varchar(30) NULL,
													image_size              int4 NULL,
	-- detail fields
	-- Removed original_data           bytea NULL,
													original_data_ds_uuid   varchar(36) NULL,
	-- Removed full_size_image         bytea NULL,
													full_size_image_ds_uuid varchar(36) NULL,
													CONSTRAINT additional_condition_upload_pk PRIMARY KEY (id),
													CONSTRAINT additional_condition_upload_additional_condition_fk FOREIGN KEY (additional_condition_id)
														REFERENCES public.additional_condition (id)
);

CREATE INDEX idx_additional_condition_upload_condition_id
	ON public.additional_condition_upload (additional_condition_id);

CREATE INDEX idx_additional_condition_upload_licence_id
	ON public.additional_condition_upload (licence_id);

-- Copy data from to other table over to new table

INSERT INTO public.additional_condition_upload (
	additional_condition_id,
	licence_id,
	filename,
	file_type,
	file_size,
	uploaded_time,
	description,
	-- Removed thumbnail_image,
	thumbnail_image_ds_uuid,
	image_type,
	image_size,
	-- Removed original_data,
	original_data_ds_uuid,
	-- Removed full_size_image,
	full_size_image_ds_uuid
)
SELECT
	s.additional_condition_id,
	d.licence_id,
	s.filename,
	s.file_type,
	s.file_size,
	s.uploaded_time,
	s.description,
	-- Removed s.thumbnail_image,
	s.thumbnail_image_ds_uuid,
	s.image_type,
	s.image_size,
	-- Removed d.original_data,
	d.original_data_ds_uuid,
	-- Removed d.full_size_image,
	d.full_size_image_ds_uuid
FROM public.additional_condition_upload_summary s
	 JOIN public.additional_condition_upload_detail d ON d.id = s.upload_detail_id
Order by s.id;

/*
-- This can be used to check if all rows have been accounted for

	SELECT
		(SELECT COUNT(*) FROM public.additional_condition_upload_summary s
				  LEFT JOIN public.additional_condition_upload_detail d
							ON d.id = s.upload_detail_id) AS old_tables_combined_count,
		(SELECT COUNT(*) FROM public.additional_condition_upload) AS new_merged_table_count;

*/

/*
-- Drop old tables, we might leave these tables as backup for the time being and removed them when we know the data is correct

	DROP TABLE public.additional_condition_upload_summary;
	DROP TABLE public.additional_condition_upload_detail;
*/

COMMIT;
