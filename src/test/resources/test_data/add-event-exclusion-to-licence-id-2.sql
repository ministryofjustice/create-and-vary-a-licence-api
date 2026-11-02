insert into additional_condition (licence_id, condition_version, condition_category, condition_code, condition_sequence,
                                  condition_text, expanded_condition_text, condition_type)
values (2, '3.0', 'Freedom of movement', '99195049-f355-46fb-b7d8-aef87a1b19c5', 3,
        'Not to enter the area as defined by the attached map, during the period that [EVENT NAME] takes place',
        'c3t', 'AP');

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
values ((select id from additional_condition where condition_code = '99195049-f355-46fb-b7d8-aef87a1b19c5' and licence_id = 2),
		'Event-map.pdf',
		'application/pdf',
		'image/png',
		12345,
		23456,
		'Event exclusion zone',
		null,
		null,
		null
	   );
