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
                     date_created,
                     date_last_updated)
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
        'Person',
        'One',
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
        '2019-06-01 14:00:00',
        '2024-06-01 15:00:00');

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
                     version_of_id,
                     date_created,
                     date_last_updated,
                     approved_date,
                     submitted_date,
                     approved_by_name)
values (2,
        'CRD',
        'AP',
        '1.0',
        'APPROVED',
        'A1234AA',
        'BOOKNO',
        123456,
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
        '2022-02-25',
        '2022-02-25',
        '2023-02-25',
        'N01',
        'PDU1',
        'LAU1',
        'TEAM1',
        1,
        1,
        1,
        '2020-04-11 10:00:00',
        '2024-07-21 11:12:00',
        '2024-08-11 11:22:00',
        '2024-02-20 15:12:00',
        'Anne Approver');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (2, 'attendMeetings', 3, 'Attend meetings', 'PSS');


insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (1, 1, 0, 'condition 1');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (2, 1, 1, 'condition 2');

insert into bespoke_condition (id, licence_id, condition_sequence, condition_text)
values (3, 2, 0, 'condition 3');

insert into audit_event (id, licence_id, username, full_name, event_type, summary, detail, changes)
values (1, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary1', 'Detail1', null),
       (2, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary2', 'Detail2', null),
       (3, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary3', 'Detail3', null),
       (4, 2, 'USER', 'Test User', 'USER_EVENT', 'Summary4', 'Detail4', null),
       (5, 2, 'USER', 'Test User', 'USER_EVENT', 'Summary5', 'Detail5', null);

insert into licence_event (id, licence_id, event_type, username, forenames, surname, event_description, event_time)
values (1, 1, 'CREATED', 'Test User', 'Test', 'User', 'Licence created1', null),
       (2, 1, 'SUBMITTED', 'Test User', 'Test', 'User', 'Licence submitted1', null),
       (3, 1, 'CREATED', 'Test User', 'Test', 'User', 'Licence created2', null),
       (4, 2, 'SUBMITTED', 'Test User', 'Test', 'User', 'Licence submitted2', null),
       (5, 2, 'SUBMITTED', 'Test User', 'Test', 'User', 'Licence submitted3', null);


-- 2. Insert the appointment
INSERT INTO appointment (person_type,
                         person,
                         time_type,
                         time,
                         address_text,
                         telephone_contact_number,
                         alternative_telephone_contact_number,
                         date_created,
                         date_last_updated)
VALUES ('SPECIFIC_PERSON', -- person_type
        'John Smith', -- person
        'SPECIFIC_DATE_TIME', -- time_type
        '2022-02-25 10:00:00+00', -- time
        '123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA', -- address_text
        '07123456789', -- contact
        '07000000000', -- contact_alternative
        '2020-04-11 10:00:00',
        '2024-07-21 11:12:00');

-- 3. Link licence <> appointment
INSERT INTO licence_appointment (licence_id, appointment_id)
VALUES ((SELECT MAX(id) FROM licence),
        (SELECT MAX(id) FROM appointment));

-- 4. Address row
INSERT INTO address (reference,
                     first_line,
                     second_line,
                     town_or_city,
                     county,
                     postcode,
                     source)
VALUES ('550e8400-e29b-41d4-a716-446655440000',
        '123 Test Street',
        'Apt 4B',
        'Testville',
        'Testshire',
        'TE5 7AA',
        'MANUAL');

-- 5. Appointment <> Address join
INSERT INTO appointment_address (appointment_id, address_id)
VALUES ((SELECT MAX(id) FROM appointment),
        (SELECT MAX(id) FROM address));

INSERT INTO time_served_external_records (noms_id,
                                          booking_id,
                                          reason,
                                          prison_code,
                                          updated_by_ca_id,
                                          date_created,
                                          date_last_updated)
VALUES ('A1234AA',
        '123',
        'Time served licence created in NOMIS',
        'MDI',
        1,
        '2024-06-01 10:00:00',
        '2024-06-01 11:00:00'),
       ('A1234AG',
        '1234',
        'Time served served licence created in NOMIS',
        'MDI',
        1,
        '2024-06-01 10:00:00',
        '2024-06-01 11:00:00'),
       ('A1234AA',
        '1235',
        'Some other time served licence created in NOMIS',
        'MDI',
        1,
        '2024-06-02 10:00:00',
        '2024-06-02 11:00:00')
;

