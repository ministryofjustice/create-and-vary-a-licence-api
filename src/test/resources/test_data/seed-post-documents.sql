insert into licence (
    id,
    kind,
    type_code,
    version,
    status_code,
    noms_id,
    booking_no,
    booking_id,
    crn,
    pnc,
    cro,
    prison_code,
    prison_description,
    forename,
    surname,
    date_of_birth,
    conditional_release_date,
    actual_release_date,
    sentence_start_date,
    sentence_end_date,
    licence_start_date,
    licence_expiry_date,
    probation_area_code,
    probation_pdu_code,
    probation_lau_code,
    probation_team_code,
    responsible_com_id,
    created_by_com_id
)
values (
           2,
           'CRD',
           'AP',
           '1.0',
           'IN_PROGRESS',
           'A1234AA',
           'BOOKNO',
           12345,
           'CRN1',
           '2015/1234',
           'CRO1',
           'MDI',
           'Moorland (HMP)',
           'Bob',
           'Mortimer',
           '2020-10-25',
           '2022-02-12',
           '2022-02-25',
           '2020-10-11',
           '2022-02-25',
           '2022-02-25',
           '2023-02-25',
           'N01',
           'PDU1',
           'LAU1',
           'TEAM1',
           1,
           1
       );

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 2, 'std-1', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 2, 'std-2', 2, 'Do not break the law', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 2, 'std-3', 3, 'Attend meetings', 'AP');

-- Create the exclusion zone additional condition
insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (1, 2, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

-- Create the data for the exclusion zone condition
insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (2, 1, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

INSERT INTO additional_condition_upload_detail (id, licence_id, additional_condition_id, original_data, full_size_image,
                                                full_size_image_ds_uuid, original_data_ds_uuid)
VALUES (1, 17, 152,'img', 'img',
        '3db10d4d-de16-4f18-91d7-caee95b3f1ad', 'e0ba4af0-0091-47eb-be4d-93f43072fbb4'),
       (2, 18, 153,'img', 'img',
        '3db10d4d-de16-4f18-91d7-caee95b3f1ad', 'e0ba4af0-0091-47eb-be4d-93f43072fbb4'),
       (3, 19, 216,'img',
        'img',
        null, null);

INSERT INTO additional_condition_upload_summary (id, additional_condition_id, filename, file_type, file_size, uploaded_time, description, thumbnail_image, upload_detail_id, thumbnail_image_ds_uuid) VALUES (89999, 1, 'Test_map_2021-12-06_112550.pdf', 'application/pdf', 253773, '2023-12-19 08:33:57.689489 +00:00', 'Description', 'img', 17, '3db10d4d-de16-4f18-91d7-caee95b3f1ad');
