INSERT INTO additional_condition_upload_detail (
    id,
    licence_id,
    additional_condition_id,
    original_data,
    full_size_image,
    full_size_image_ds_uuid,
    original_data_ds_uuid
)
VALUES
(
    1,
    2,
    1,
    DECODE('6f726967696e616c4461746131', 'hex'),
    DECODE('66756c6c53697a65496d61676531', 'hex'),
    NULL,
    NULL
),
(
    2,
    2,
    1,
    DECODE('6f726967696e616c4461746132', 'hex'),
    DECODE('66756c6c53697a65496d61676532', 'hex'),
    NULL,
    NULL
),
(
    3,
    2,
    1,
    DECODE('6f726967696e616c4461746133', 'hex'),
    DECODE('66756c6c53697a65496d61676533', 'hex'),
    'a6f426f5-c77d-4853-be21-d675f84739bf',
    '6157b08c-9553-45ad-9610-0a22f8cf8dc6'
);

INSERT INTO additional_condition_upload_summary (
id,
additional_condition_id,
thumbnail_image,
thumbnail_image_ds_uuid,
upload_detail_id,
file_size
)
VALUES
(
    1,
    1,
    DECODE('7468756d626e61696c496d61676531', 'hex'),
    NULL,
    1,
    0
),
(
    2,
    1,
    DECODE('7468756d626e61696c496d61676532', 'hex'),
    NULL,
    2,
    0
),
(
    3,
    1,
    DECODE('7468756d626e61696c496d61676533', 'hex'),
    '4ac3d2d8-2a1c-48a0-bcc7-3d43ee7d9a69',
    3,
    0
);