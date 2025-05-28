insert into licence (id,
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
                     created_by_com_id)
values (1,
        'VARIATION',
        'AP',
        '1.0',
        'VARIATION_APPROVED',
        'A1234AA',
        'BOOKNO',
        12345,
        'CRN1',
        '2015/1234',
        'CRO1',
        'MDI',
        'Moorland (HMP)',
        'Person',
        'One',
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
        1);

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 1, 'std-1', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 1, 'std-2', 2, 'Do not break the law', 'AP');

insert into standard_condition (id, licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 1, 'std-3', 3, 'Attend meetings', 'AP');

-- Create the exclusion zone additional condition
insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (1, 1, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1,
        'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]', 'AP');

-- Create the data for the exclusion zone condition
insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (2, 1, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');
