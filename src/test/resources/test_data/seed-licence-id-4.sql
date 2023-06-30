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
    topup_supervision_expiry_date,
    probation_area_code,
    probation_pdu_code,
    probation_lau_code,
    probation_team_code,
    responsible_com_id,
    created_by_com_id
)
values (
    3,
    'AP',
    '1.0',
    'ACTIVE',
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
    CURRENT_DATE - 1,
    CURRENT_DATE + 1,
    'N01',
    'PDU1',
    'LAU1',
    'TEAM1',
    1,
    1
 );

-- Create the exclusion zone additional condition
insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (1, 3, '1.0', 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (2, 3, '1.0', 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'PSS');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (3, 3, '1.0', 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (4, 3, '1.0', 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'PSS');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
values (5, 3, '1.0', 'Freedom of movement', 'code-1', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

-- Create the data for the exclusion zone condition
insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (2, 1, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (3, 2, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (4, 2, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (5, 3, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (6, 3, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (7, 4, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (8, 4, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (9, 5, 1, 'outOfBoundArea', 'Plymouth town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (10, 5, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');
