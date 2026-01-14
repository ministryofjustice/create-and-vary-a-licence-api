INSERT INTO additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
VALUES
    (1, 2, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP'),
    (2, 2, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP'),
    (3, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

INSERT INTO additional_condition_upload (
    id,
    additional_condition_id,
	full_size_image_ds_uuid,
	original_data_ds_uuid,
    thumbnail_image_ds_uuid,
    file_size,
    file_type
)
VALUES
    (1,
        1,
	 	'37eb7e31-a133-4259-96bc-93369b917eb8',
	 	'37eb7e31-a133-4259-96bc-93369b917eb8',
        '37eb7e31-a133-4259-96bc-93369b917eb8',
        1,
        'image/png'
    ),
    (2,
        3,
	 	'37eb7e31-a133-4259-96bc-93369b917eb8',
	 	'20ca047a-0ae6-4c09-8b97-e3f211feb733',
        '53655fe1-1368-4ed3-bfb0-2727a4e73be5',
        1,
        'image/png'
    ),
    ( 3,
        2,
	  	'20ca047a-0ae6-4c09-8b97-e3f211feb733',
	  	'53655fe1-1368-4ed3-bfb0-2727a4e73be5',
        '53655fe1-1368-4ed3-bfb0-2727a4e73be5',
        1,
        'image/png'
    );

ALTER SEQUENCE additional_condition_upload_id_seq RESTART WITH 4;
