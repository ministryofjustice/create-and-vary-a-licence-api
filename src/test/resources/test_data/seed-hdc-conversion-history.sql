insert into licence (
  kind, eligible_kind, type_code, version, status_code, noms_id, booking_id,
  forename, surname, licence_start_date, responsible_com_id, created_by_com_id, licence_version
)
values
  ('HDC', 'HDC', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0'),
  ('HDC', 'HDC', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0'),
  ('CRD', 'CRD', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0'),
  ('CRD', 'CRD', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0'),
  ('HDC', 'HDC', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0'),
  ('HDC', 'HDC', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123,
   'Person', 'One', current_date, 1, 1, '1.0');

insert into curfew_times (curfew_times_sequence, from_day, from_time, until_day, until_time)
values
  (1, 'MONDAY', '19:00:00', 'TUESDAY', '07:00:00'),
  (2, 'TUESDAY', '20:00:00', 'WEDNESDAY', '08:00:00'),
  (null, null, '21:00:00', null, '06:00:00');

insert into hdc_weekly_curfew_times (licence_id, curfew_time_id)
values (2, 1), (2, 2);

insert into hdc_first_night_curfew_times (licence_id, curfew_time_id)
values (2, 3);

insert into hdc_curfew_address (
  licence_id, reference, uprn, first_line, second_line, town_or_city, county, postcode,
  source, accommodation_type, post_release_residential_checks_completed,
  post_release_residential_checks_not_completed_reason
)
values
  (2, 'older-address', null, '1 Older Street', null, 'Old Town', null, 'OL1 1AA',
   'HDC_CREATION', 'RESIDENTIAL', null, null),
  (3, 'previous-hdc-address', '123456789', '42 Previous Street', 'Flat 2', 'Previous Town',
   'Previous County', 'PR1 2AB', 'OS_PLACES', 'RESIDENTIAL', false, 'Checks pending');

insert into electronic_monitoring_provider (licence_id, is_to_be_tagged_for_programme, programme_name)
values (3, true, 'Previous HDC Programme');

insert into bespoke_condition (licence_id, condition_sequence, condition_text)
values (3, 1, 'Old HDC condition which must not replace the current CRD conditions');
