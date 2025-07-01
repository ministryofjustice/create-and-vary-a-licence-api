insert into additional_condition_upload_detail
    (id, licence_id, additional_condition_id, original_data, full_size_image, full_size_image_ds_uuid, original_data_ds_uuid)
values
(
    1,
    2,
    1,
    decode('6f726967696e616c4461746131', 'hex'),
    decode('66756c6c53697a65496d61676531', 'hex'),
    null,
    null
),
(
    2,
    2,
    1,
    decode('6f726967696e616c4461746132', 'hex'),
    decode('66756c6c53697a65496d61676532', 'hex'),
    null,
    null
)
;
insert into additional_condition_upload_summary
    (id, additional_condition_id, thumbnail_image, thumbnail_image_ds_uuid, upload_detail_id, file_size)
values
(
    1,
    1,
    decode('7468756d626e61696c496d61676531', 'hex'),
    null,
    1,
    0
),
(
    2,
    1,
    decode('7468756d626e61696c496d61676532', 'hex'),
    null,
    1,
    0
)
;