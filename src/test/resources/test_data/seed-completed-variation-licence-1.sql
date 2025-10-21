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
		   'VARIATION',
		   'AP',
		   '1.0',
		   'IN_PROGRESS',
		   'B1234BB',
		   'BOOKNO2',
		   12346,
		   'CRN2',
		   '2016/1234',
		   'CRO2',
		   'MDI',
		   'Moorland (HMP)',
		   'Person',
		   'Two',
		   '1990-05-15',
		   '2023-01-10',
		   '2023-01-20',
		   '2022-12-01',
		   '2023-01-20',
		   '2023-01-20',
		   '2023-01-20',
		   current_date,
		   '2023-12-20',
		   'N01',
		   'PDU2',
		   'LAU2',
		   'TEAM2',
		   1,
		   1,
		   '1.0'
	   );

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'attendMeetings', 3, 'Attend meetings', 'PSS');

insert into electronic_monitoring_provider (licence_id, is_to_be_tagged_for_programme, programme_name)
VALUES (1, false, 'Variation Programme');
