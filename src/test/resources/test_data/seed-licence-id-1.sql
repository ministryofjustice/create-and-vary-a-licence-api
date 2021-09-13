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
  com_first_name,
  com_last_name,
  com_username,
  com_staff_id,
  com_email,
  com_telephone,
  probation_area_code,
  probation_ldu_code
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
  'Stephen',
  'Mills',
  'X12345',
  123456,
  'stephen.mills@nps.gov.uk',
  '0114 2765666',
  'N01',
  'LDU1'
);

insert into standard_term (licence_id, term_code, term_sequence, term_text)
values (1, 'goodBehaviour', 1, 'Be of generally good behaviour');

insert into standard_term (licence_id, term_code, term_sequence, term_text)
values (1, 'notBreakLaw', 2, 'Do not break the law');

insert into standard_term (licence_id, term_code, term_sequence, term_text)
values (1, 'attendMeetings', 3, 'Attend meetings');
