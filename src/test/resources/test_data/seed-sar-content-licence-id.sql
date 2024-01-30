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
                     topup_supervision_start_date,
                     topup_supervision_expiry_date,
                     licence_start_date,
                     licence_expiry_date,
                     probation_area_code,
                     probation_pdu_code,
                     probation_lau_code,
                     probation_team_code,
                     responsible_com_id,
                     created_by_com_id)
values (1,
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
        '2022-02-25',
        '2022-02-25',
        '2023-02-25',
        'N01',
        'PDU1',
        'LAU1',
        'TEAM1',
        1,
        1);

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'attendMeetings', 3, 'Attend meetings', 'PSS');

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
                     topup_supervision_start_date,
                     topup_supervision_expiry_date,
                     licence_start_date,
                     licence_expiry_date,
                     probation_area_code,
                     probation_pdu_code,
                     probation_lau_code,
                     probation_team_code,
                     responsible_com_id,
                     created_by_com_id,
                     version_of_id)
values (2,
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
        '2022-02-25',
        '2022-02-25',
        '2023-02-25',
        'N01',
        'PDU1',
        'LAU1',
        'TEAM1',
        1,
        1,
        1);

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'attendMeetings', 3, 'Attend meetings', 'PSS');

insert into audit_event (id, licence_id, username, full_name, event_type, summary, detail, changes)
values (1, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary1', 'Detail1', null),
       (2, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary2', 'Detail2', null),
       (3, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary3', 'Detail3', null),
       (4, 2, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary4', 'Detail4', null),
       (5, 2, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary5', 'Detail5', null);

insert into licence_event (id, licence_id, event_type, username, forenames, surname, event_description, event_time)
values (1, 1, 'CREATED', 'Bob Smith', 'Bob', 'Smith', 'Licence created1', null),
       (2, 1, 'SUBMITTED', 'Bob Smith', 'Bob', 'Smith', 'Licence submitted1', null),
       (3, 1, 'CREATED', 'Bob Smith', 'Bob', 'Smith', 'Licence created2', null),
       (4, 2, 'SUBMITTED', 'Bob Smith', 'Bob', 'Smith', 'Licence submitted2', null),
       (5, 2, 'SUBMITTED', 'Bob Smith', 'Bob', 'Smith', 'Licence submitted3', null);


