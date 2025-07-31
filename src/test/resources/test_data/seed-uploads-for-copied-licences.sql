INSERT INTO additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
VALUES
    (1, 2, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP'),
    (2, 2, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP'),
    (3, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

INSERT INTO additional_condition_upload_detail (
    id,
    licence_id,
    additional_condition_id,
    full_size_image_ds_uuid,
    original_data_ds_uuid
)
VALUES
    (
        1,
        2,
        1,
        '37eb7e31-a133-4259-96bc-93369b917eb8',
        '1595ef41-36e0-4fa8-a98b-bce5c5c98220'
    ),
    (
        2,
        3,
        3,
        '37eb7e31-a133-4259-96bc-93369b917eb8',
        '1595ef41-36e0-4fa8-a98b-bce5c5c98220'
    ),
    (
        3,
        2,
        2,
        '20ca047a-0ae6-4c09-8b97-e3f211feb733',
        '53655fe1-1368-4ed3-bfb0-2727a4e73be5'
    );

INSERT INTO additional_condition_upload_summary (
    additional_condition_id,
    thumbnail_image_ds_uuid,
    upload_detail_id
)
VALUES
    (
        1,
        '92939445-4159-4214-aa75-d07568a3e136',
        1
    ),
    (
        3,
        '0bbf1459-ee7a-4114-b509-eb9a3fcc2756',
        2
    ),
    (
        2,
        '53655fe1-1368-4ed3-bfb0-2727a4e73be5',
        3
    );