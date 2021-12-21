insert into additional_condition_upload_summary
  (id, additional_condition_id, filename, file_type, file_size, description, thumbnail_image, upload_detail_id)
values
  (1, 1, 'Test-file.pdf', 'application/pdf', 12345, 'Description', STRINGTOUTF8('thumb'), 1);

insert into additional_condition_upload_detail
  (id, licence_id, additional_condition_id, original_data, full_size_image)
values
   (1, 2, 1, STRINGTOUTF8('Some data'), STRINGTOUTF8('some more data'));

