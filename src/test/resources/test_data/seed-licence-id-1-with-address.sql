-- 1. Insert the licence
INSERT INTO licence (
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
	licence_version
) VALUES (
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
			 '1.0'
		 );

-- 2. Insert the appointment
INSERT INTO appointment (
	person_type,
	person,
	time_type,
	time,
	address_text,
	telephone_contact_number,
	alternative_telephone_contact_number,
	date_created,
	date_last_updated
) VALUES (
			 'SPECIFIC_PERSON',           -- person_type
			 'John Smith',                -- person
			 'SPECIFIC_DATE_TIME',        -- time_type
			 '2022-02-25 10:00:00+00',    -- time
			 '123 Test Street,Apt 4B,Testville,Testshire,TE5 7AA', -- address_text
			 '07123456789',               -- contact
			 '07000000000',               -- contact_alternative
			 current_timestamp,           -- date_created
			 current_timestamp           -- date_last_updated
		 );

-- 3. Link licence <> appointment
INSERT INTO licence_appointment (licence_id, appointment_id)
VALUES (
		   (SELECT MAX(id) FROM licence),
		   (SELECT MAX(id) FROM appointment)
	   );

-- 4. Address row
INSERT INTO address (
	reference,
	first_line,
	second_line,
	town_or_city,
	county,
	postcode,
	source
) VALUES (
			 '550e8400-e29b-41d4-a716-446655440000',
			 '123 Test Street',
			 'Apt 4B',
			 'Testville',
			 'Testshire',
			 'TE5 7AA',
			 'MANUAL'
		 );

-- 5. Appointment <> Address join
INSERT INTO appointment_address (appointment_id, address_id)
VALUES (
		   (SELECT MAX(id) FROM appointment),
		   (SELECT MAX(id) FROM address)
	   );

-- 6. Standard conditions
INSERT INTO standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
VALUES ((SELECT MAX(id) FROM licence), 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

INSERT INTO standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
VALUES ((SELECT MAX(id) FROM licence), 'notBreakLaw', 2, 'Do not break the law', 'AP');

INSERT INTO standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
VALUES ((SELECT MAX(id) FROM licence), 'attendMeetings', 3, 'Attend meetings', 'PSS');

-- 7. Bespoke condition
INSERT INTO bespoke_condition (licence_id, condition_sequence, condition_text)
VALUES ((SELECT MAX(id) FROM licence), 4, 'bespoke condition 1');

-- 8. Electronic monitoring
INSERT INTO electronic_monitoring_provider (licence_id, is_to_be_tagged_for_programme, programme_name)
VALUES ((SELECT MAX(id) FROM licence), true, 'Test Programme');

-- 9. Additional condition + data + upload
INSERT INTO additional_condition (licence_id, condition_version, condition_category, condition_code, condition_sequence, condition_text, condition_type)
VALUES (
		   (SELECT MAX(id) FROM licence),
		   '1.0',
		   'Freedom of movement',
		   '9ae2a336-3491-4667-aaed-dd852b09b4b9',
		   5,
		   'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
		   'AP'
	   );

INSERT INTO additional_condition_data (additional_condition_id, data_sequence, data_field, data_value)
VALUES (
		   (SELECT MAX(id) FROM additional_condition),
		   1,
		   'outOfBoundArea',
		   'Town centre'
	   );

INSERT INTO additional_condition_upload_summary (
	additional_condition_id,
	filename,
	file_type,
	image_type,
	file_size,
	image_size,
	description,
	thumbnail_image,
	upload_detail_id
) VALUES (
			 (SELECT MAX(id) FROM additional_condition),
			 'Test-file.pdf',
			 'application/pdf',
			 'image/png',
			 12345,
			 23456,
			 'Description',
			 'thumb',
			 1
		 );

INSERT INTO additional_condition_upload_detail (
	licence_id,
	additional_condition_id,
	original_data,
	full_size_image
) VALUES (
			 (SELECT MAX(id) FROM licence),
			 (SELECT MAX(id) FROM additional_condition),
			 'Some data',
			 'some more data'
		 );

-- 10. Another address + staff_saved_appointment_address
INSERT INTO address (
	reference,
	first_line,
	second_line,
	town_or_city,
	county,
	postcode,
	source
) VALUES (
			 '550e8400-e29b-41d4-a716-446655440001',
			 '122 Test Street',
			 'Apt 4B',
			 'Testville',
			 'Testshire',
			 'TE5 7AA',
			 'MANUAL'
		 );

INSERT INTO staff_saved_appointment_address (staff_id, address_id)
VALUES (1, (SELECT MAX(id) FROM address));
