insert into additional_condition_upload_summary
(additional_condition_id, filename, file_type, image_type, file_size, description, thumbnail_image, upload_detail_id)
values (1, 'Test-file.pdf', 'application/pdf', 'image/png', 12345, 'Description', 'thumb', 1);

insert into additional_condition_upload_detail
    (licence_id, additional_condition_id, original_data, full_size_image)
values (2, 1, 'Some data', 'some more data');

