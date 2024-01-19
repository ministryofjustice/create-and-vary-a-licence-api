ALTER TABLE additional_condition_upload_detail ADD COLUMN full_size_image_ds_uuid VARCHAR(36);
ALTER TABLE additional_condition_upload_detail ADD COLUMN original_data_ds_uuid VARCHAR(36);
ALTER TABLE additional_condition_upload_summary ADD COLUMN thumbnail_image_ds_uuid  VARCHAR(36);