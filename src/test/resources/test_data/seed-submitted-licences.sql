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
  licence_start_date,
  type_code
) values
(1,'CRD','1.0','100','A1234AA','MDI',1,1,1,'SUBMITTED', 'jim', 'smith', '2022-09-29','AP'),
(2,'CRD','1.0','A12345','A1234AB','ABC',1,1,1,'SUBMITTED','joe','bloggs', '2022-01-01','AP'),
(3,'CRD','1.0','300','A1234AC','MDI',1,1,2,'SUBMITTED','john', 'doe',CURRENT_TIMESTAMP,'AP'),
(4,'CRD','1.0','400','A1234AD','MDI',1,1,1,'APPROVED','bob', 'bobson', '2023-09-29','AP'),
(5,'CRD','1.0','500','A1234AE','MDI',1,1,1,'APPROVED','prisoner', 'five', '2023-09-29','AP'),
(6,'CRD','1.0','600',null,'ABC',1,1,1,'SUBMITTED','prisoner', 'six', '2023-09-29','AP'),
(7,'CRD','1.0','B12345','A1234BC','ABC',1,1,1,'SUBMITTED','prisoner', 'seven', null,'AP'),
(8,'CRD','1.0','C12345','B1234BC','ABC',1,1,1,'SUBMITTED','prisoner', 'eight', '2024-03-14','AP');

