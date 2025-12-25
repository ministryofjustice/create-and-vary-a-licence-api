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
    'VARIATION',
    'AP_PSS',
    '1.0',
    'VARIATION_IN_PROGRESS',
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
    CURRENT_DATE - 1,
    CURRENT_DATE + 1,
    'N01',
    'PDU1',
    'LAU1',
    'TEAM1',
    1,
    1
 ),
   (
       4,
       'VARIATION',
       'AP_PSS',
       '1.0',
       'VARIATION_IN_PROGRESS',
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
       CURRENT_DATE - 1,
       CURRENT_DATE + 1,
       'N01',
       'PDU1',
       'LAU1',
       'TEAM1',
       1,
       1),
       (5,
        'VARIATION',
        'AP_PSS',
        '1.0',
        'VARIATION_IN_PROGRESS',
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
        CURRENT_DATE + 1,
        CURRENT_DATE + 1,
        'N01',
        'PDU1',
        'LAU1',
        'TEAM1',
        1,
        1),
       (6,
        'CRD',
        'AP_PSS',
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
        'Person',
        'One',
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
        1);

-- Create the AP and PSS standard condition
insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (3, 'attendMeetings', 1, 'Attend meetings', 'PSS');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (4, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (4, 'notBreakLaw', 1, 'Do not break the law', 'PSS');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (4, 'attendMeetings', 2, 'Attend meetings', 'PSS');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (5, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (5, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (5, 'attendMeetings', 1, 'Attend meetings', 'PSS');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (6, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (6, 'attendMeetings', 1, 'Attend meetings', 'PSS');

-- Create the AP and PSS additional condition
insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (1, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (2, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'PSS');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (3, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (4, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'PSS');



insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (5, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (6, 4, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (7, 4, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 1, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'PSS');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (8, 5, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 3, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (9, 5, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'PSS');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (10, 6, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 3, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'AP');

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, condition_type)
values (11, 6, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
        'PSS');

-- For Uploaded File
insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
								  condition_sequence, condition_text, condition_type)
values (12, 3, '1.0', 'Freedom of movement', '9ae2a336-3491-4667-aaed-dd852b09b4b9', 2, 'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
		'AP');

INSERT INTO additional_condition_upload (id,
                                                 additional_condition_id,
												 full_size_image_ds_uuid,
												 original_data_ds_uuid,
                                                 thumbnail_image_ds_uuid,
                                                 file_size, file_type)
VALUES (
		1,12,
		'37eb7e31-a133-4259-96bc-93369b917eb8',
		'1595ef41-36e0-4fa8-a98b-bce5c5c98220',
        '92939445-4159-4214-aa75-d07568a3e136',
		1,
        'image/png'
	   );

-- Create the data for the exclusion zone condition
insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (2, 1, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (3, 2, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (4, 2, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (5, 3, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (6, 3, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (7, 4, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (8, 4, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (9, 5, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (10, 5, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (11, 6, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (12, 7, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (13, 8, 1, 'outOfBoundArea', 'Town centre');

insert into additional_condition_data (id, additional_condition_id, data_sequence, data_field, data_value)
values (14, 9, 2, 'outOfBoundFile', 'Test_map_2021-12-06_112550.pdf');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (1, 3, 0, 'condition 1');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (2, 3, 0, 'condition 2');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (3, 3, 0, 'condition 3');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (4, 4, 0, 'condition 1');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (5, 4, 0, 'condition 2');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (6, 5, 0, 'condition 1');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (7, 6, 0, 'condition 1');
