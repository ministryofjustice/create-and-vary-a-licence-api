UPDATE additional_condition_upload_summary AS acus
SET image_size = octet_length(full_size_image)
    FROM additional_condition_upload_detail AS acud
where
    acus.upload_detail_id = acud.id
  and
    acus.id in (
    SELECT id
    FROM   additional_condition_upload_summary
    WHERE  image_size is null
    LIMIT  1
    );

