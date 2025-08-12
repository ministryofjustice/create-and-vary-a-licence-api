insert into licence (kind,
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
                     licence_version,
					 appointment_address)
values (
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
        current_date,
        '2023-02-25',
        'N01',
        'PDU1',
        'LAU1',
        'TEAM1',
        1,
        1,
        '1.0',
        '123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'attendMeetings', 3, 'Attend meetings', 'PSS');

insert into electronic_monitoring_provider (licence_id, is_to_be_tagged_for_programme, programme_name)
VALUES (1, true, 'Test Programme');

INSERT INTO address (reference, first_line, second_line, town_or_city, county, postcode, source)
VALUES ('550e8400-e29b-41d4-a716-446655440000', '123 Test Street', 'Apt 4B', 'Testville', 'Testshire', 'TE5 7AA','MANUAL');

INSERT INTO licence_appointment_address(licence_id,address_id) VALUES(1,1);
INSERT INTO staff_saved_appointment_address(staff_id,address_id) VALUES(1,1);
