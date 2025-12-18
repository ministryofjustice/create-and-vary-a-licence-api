insert into additional_condition_upload_summary
(additional_condition_id, filename, file_type, image_type, file_size, image_size, description, thumbnail_image,
 upload_detail_id)
values (1, 'Test-file.pdf', 'application/pdf', 'image/png', 12345, 23456, 'Description', 'thumb', 1);

insert into additional_condition_upload_detail
    (id, licence_id, additional_condition_id, original_data, full_size_image, full_size_image_ds_uuid)
values (1, 2, 1, 'Some data', 'some more data','44f8163c-6c97-4ff2-932b-ae24feb0c112');
