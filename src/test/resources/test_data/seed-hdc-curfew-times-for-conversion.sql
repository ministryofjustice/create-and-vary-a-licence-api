insert into curfew_times (curfew_times_sequence, from_day, from_time, until_day, until_time)
values
  (1, 'MONDAY', '19:00:00', 'TUESDAY', '07:00:00'),
  (2, 'TUESDAY', '20:00:00', 'WEDNESDAY', '08:00:00'),
  (null, null, '21:00:00', null, '06:00:00');

insert into hdc_weekly_curfew_times (licence_id, curfew_time_id)
select (select max(id) from licence), id
from curfew_times where curfew_times_sequence is not null;

insert into hdc_first_night_curfew_times (licence_id, curfew_time_id)
select (select max(id) from licence), id
from curfew_times where curfew_times_sequence is null;
