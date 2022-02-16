insert into licence (
  id,
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
  created_by_com_id
)
values (
  1,
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
  '2023-02-25',
  'N01',
  'PDU1',
  'LAU1',
  'TEAM1',
  1,
  1
);

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'goodBehaviour', 1, 'Be of generally good behaviour', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'notBreakLaw', 2, 'Do not break the law', 'AP');

insert into standard_condition (licence_id, condition_code, condition_sequence, condition_text, condition_type)
values (1, 'attendMeetings', 3, 'Attend meetings', 'PSS');
