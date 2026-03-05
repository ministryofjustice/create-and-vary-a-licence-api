-- 1. Insert AP_PSS licence (this is what job will process)
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
			 'AP_PSS',
			 '1.0',
			 'APPROVED',
			 'A1234AA',
			 'BOOKNO-ISR',
			 54321,
			 'CRN-ISR',
			 '2015/1234',
			 'CRO-ISR',
			 'MDI',
			 'Moorland (HMP)',
			 'ISR',
			 'PSS Progression',
			 '1980-01-01',
			 '2022-02-12',
			 '2022-02-25',
			 '2020-10-11',
			 '2022-02-25',
			 '2026-05-01',
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

-- 2. Insert PSS standard condition (should be removed by job)
INSERT INTO standard_condition (
	licence_id,
	condition_code,
	condition_sequence,
	condition_text,
	condition_type
) VALUES (
			 (SELECT max(id) FROM licence),
			 'pssConditionStd',
			 1,
			 'PSS Standard Condition',
			 'PSS'
		 );

-- 3. Insert PSS additional condition (should be removed by job)
INSERT INTO additional_condition (
	licence_id,
	condition_version,
	condition_category,
	condition_code,
	condition_sequence,
	condition_text,
	condition_type
) VALUES (
			 (SELECT max(id) FROM licence),
             '1.0',
          '(Additional) Freedom of movement',
			 'pssConditionAdd',
			 1,
			 'PSS Additional Condition',
			 'PSS'
		 );
