insert into licence (
  id,
  crn,
  responsible_com_id,
  created_by_com_id,
  submitted_by_com_id,
  status_code,
  forename,
  surname,
  conditional_release_date,
  actual_release_date
) values
(1,'100',1,1,1,'SUBMITTED', 'jim', 'smith', CURRENT_TIMESTAMP,'2022-09-29'),
(2,'200',1,1,1,'SUBMITTED','harry','hope', CURRENT_TIMESTAMP,'2022-01-01'),
(3,'300',1,1,2,'SUBMITTED','terry', 'towel','2022-09-29',CURRENT_TIMESTAMP),
(4,'400',1,1,1,'APPROVED','bob', 'pint', CURRENT_TIMESTAMP,'2023-09-29'),
(5,'500',1,1,1,'APPROVED','prisoner', 'five', '2023-09-29','2023-09-29'),
(6,'600',1,1,1,'SUBMITTED','prisoner', 'six', CURRENT_TIMESTAMP,'2023-09-29');
