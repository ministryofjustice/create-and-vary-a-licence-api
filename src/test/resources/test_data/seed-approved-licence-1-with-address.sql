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
			 'APPROVED',
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
			 '07123456789',               -- telephone_contact_number
			 '07000000000',               -- alternative_telephone_contact_number
			 current_timestamp,           -- date_created
			 current_timestamp           -- date_last_updated
		 );

-- 3. Link licence <> appointment
INSERT INTO licence_appointment (licence_id, appointment_id)
VALUES (
		   (SELECT max(id) FROM licence),
		   (SELECT max(id) FROM appointment)
	   );

-- 4. Insert the address
INSERT INTO address (
	reference,
	first_line,
	second_line,
	town_or_city,
	county,
	postcode,
	source
) VALUES (
			 'REF-123456',
			 '123 Test Street',
			 'Apt 4B',
			 'Testville',
			 'Testshire',
			 'TE5 7AA',
			 'MANUAL'
		 );

-- 5. Link appointment to address
INSERT INTO appointment_address (appointment_id, address_id)
VALUES (
		   (SELECT max(id) FROM appointment),
		   (SELECT max(id) FROM address)
	   );

-- 6. Standard conditions
INSERT INTO standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
VALUES
	((SELECT max(id) FROM licence), 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP'),
	((SELECT max(id) FROM licence), 'notBreakLaw', 2, 'Do not break the law', 'AP'),
	((SELECT max(id) FROM licence), 'attendMeetings', 3, 'Attend meetings', 'PSS');
