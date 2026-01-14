insert into additional_condition_upload (
	additional_condition_id,
	filename,
	file_type,
	image_type,
	file_size,
	image_size,
	description,
	thumbnail_image_ds_uuid,
	original_data_ds_uuid,
	full_size_image_ds_uuid
)
values (1,
		'Test-file.pdf',
		'application/pdf',
		'image/png',
		12345,
		23456,
		'Description',
		null,
		null,
		'44f8163c-6c97-4ff2-932b-ae24feb0c112'
        );
