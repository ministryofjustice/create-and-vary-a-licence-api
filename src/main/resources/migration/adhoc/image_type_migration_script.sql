
UPDATE additional_condition_upload_summary AS acus
SET image_type = (CASE
                     WHEN ENCODE(SUBSTRING(full_size_image FROM 1 FOR 8), 'hex') = '89504e470d0a1a0a' THEN 'image/png'
                     WHEN ENCODE(SUBSTRING(full_size_image FROM 1 FOR 2), 'hex') = 'ffd8'
                         AND ENCODE(SUBSTRING(full_size_image FROM LENGTH(full_size_image) - 1 FOR 2), 'hex') = 'ffd9' THEN 'image/jpeg'
                     ELSE null
    END)
FROM additional_condition_upload_detail AS acud
where id = (
    SELECT id
    FROM   additional_condition_upload_summary
    WHERE  image_type = null
    LIMIT  10
)
