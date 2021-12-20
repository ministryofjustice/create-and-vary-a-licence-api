insert into licence (
    id,
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
    com_first_name,
    com_last_name,
    com_username,
    com_staff_id,
    com_email,
    com_telephone,
    probation_area_code,
    probation_ldu_code
)
values (
    2,
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
    'Stephen',
    'Mills',
    'X12345',
    123456,
    'stephen.mills@nps.gov.uk',
    '0114 2765666',
    'N01',
    'LDU1'
 );

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 2, 'std-1', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 2, 'std-2', 2, 'Do not break the law', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 2, 'std-3', 3, 'Attend meetings', 'AP');

-- Create the exclusion zone additional condition
insert into additional_condition (id, licence_id, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (1, 2, 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

-- Create the data for the exclusion zone condition
insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (2, 1, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');
