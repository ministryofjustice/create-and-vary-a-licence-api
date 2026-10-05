insert into licence (
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
)
values (
		   'HDC',
		   'AP',
		   '1.0',
		   'IN_PROGRESS',
		   'C1234CC',
		   'BOOKNO3',
		   12347,
		   'CRN3',
		   '2017/1234',
		   'CRO3',
		   'MDI',
		   'Moorland (HMP)',
		   'Person',
		   'Three',
		   '1985-08-20',
		   '2023-06-15',
		   '2023-06-25',
		   '2023-01-01',
		   '2023-06-25',
		   '2023-06-25',
		   '2023-06-25',
		   current_date,
		   '2024-01-10',
		   'N02',
		   'PDU3',
		   'LAU3',
		   'TEAM3',
		   1,
		   1,
		   '1.0'
	   );

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values ((select max(id) from licence), 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values ((select max(id) from licence), 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values ((select max(id) from licence), 'attendMeetings', 3, 'Attend meetings', 'PSS');

insert into electronic_monitoring_provider (licence_id, is_to_be_tagged_for_programme, programme_name)
VALUES ((select max(id) from licence), true, 'HDC Programme');

insert into probation_contact (
  appointment_type,
  person,
  appointment_time_type,
  appointment_time,
  address_text,
  telephone_contact_number,
  alternative_telephone_contact_number,
  date_created,
  date_last_updated
)
values (
  'SPECIFIC_PERSON',
  'Probation Officer IP',
  'SPECIFIC_DATE_TIME',
  '2026-10-01 10:00:00+00',
  '1 Probation Street, London',
  '07123456789',
  '07000000000',
  current_timestamp,
  current_timestamp
);

insert into licence_probation_contact (licence_id, probation_contact_id)
values ((select max(id) from licence), (select max(id) from probation_contact));
