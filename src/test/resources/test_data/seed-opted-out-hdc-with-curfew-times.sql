insert into licence (
  kind, eligible_kind, type_code, version, status_code, noms_id, booking_id,
  forename, surname, licence_start_date, responsible_com_id, created_by_com_id, licence_version
)
values
  ('HDC', 'HDC', 'AP', '1.0', 'INACTIVE', 'A1234AA', 123, 'Person', 'One', current_date, 1, 1, '1.0'),
  ('CRD', 'CRD', 'AP', '1.0', 'IN_PROGRESS', 'A1234AA', 123, 'Person', 'One', current_date, 1, 1, '1.0');

insert into curfew_times (curfew_times_sequence, from_day, from_time, until_day, until_time)
values
  (1, 'MONDAY', '19:00:00', 'TUESDAY', '07:00:00'),
  (2, 'TUESDAY', '20:00:00', 'WEDNESDAY', '08:00:00'),
  (null, null, '21:00:00', null, '06:00:00');

insert into hdc_weekly_curfew_times (licence_id, curfew_time_id)
values (1, 1), (1, 2);

insert into hdc_first_night_curfew_times (licence_id, curfew_time_id)
values (1, 3);

insert into licence_link (from_licence_id, to_licence_id, link_type, date_created)
values (1, 2, 'CRD_REPLACEMENT_FOR_OPTED_OUT_LICENCE', '2026-10-04 12:00:00');
