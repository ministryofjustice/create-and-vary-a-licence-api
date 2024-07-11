insert into licence (
  id,
  kind,
  version,
  crn,
  noms_id,
  prison_code,
  responsible_com_id,
  created_by_com_id,
  submitted_by_com_id,
  status_code,
  forename,
  surname,
  conditional_release_date,
  actual_release_date,
  type_code
) values
(1,'CRD','1.0','100','A1234AA','MDI',1,1,1,'SUBMITTED', 'jim', 'smith', CURRENT_TIMESTAMP,'2022-09-29','AP'),
(2,'CRD','1.0','200','A1234AB','ABC',1,1,1,'SUBMITTED','harry','hope', CURRENT_TIMESTAMP,'2022-01-01','AP'),
(3,'CRD','1.0','300','A1234AC','MDI',1,1,2,'SUBMITTED','terry', 'towel','2022-09-29',CURRENT_TIMESTAMP,'AP'),
(4,'CRD','1.0','400','A1234AD','MDI',1,1,1,'APPROVED','bob', 'pint', CURRENT_TIMESTAMP,'2023-09-29','AP'),
(5,'CRD','1.0','500','A1234AE','MDI',1,1,1,'APPROVED','prisoner', 'five', '2023-09-29','2023-09-29','AP'),
(6,'CRD','1.0','600',null,'ABC',1,1,1,'SUBMITTED','prisoner', 'six', CURRENT_TIMESTAMP,'2023-09-29','AP'),
(7,'CRD','1.0','700','A1234BC','ABC',1,1,1,'SUBMITTED','prisoner', 'seven', null, null,'AP'),
(8,'CRD','1.0','800','B1234BC','ABC',1,1,1,'SUBMITTED','prisoner', 'eight', '2024-03-14', null,'AP');

