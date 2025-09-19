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
(1,'CRD','1.0','100','A1234AA','MDI',1,1,1,'SUBMITTED', 'Person', 'One', '2022-09-29','AP'),
(2,'CRD','1.0','A12345','A1234AB','ABC',1,1,1,'SUBMITTED','Person','Two', '2022-01-01','AP'),
(3,'CRD','1.0','300','A1234AC','MDI',1,1,2,'SUBMITTED','Person', 'Three',CURRENT_TIMESTAMP,'AP'),
(4,'CRD','1.0','400','A1234AD','MDI',1,1,1,'APPROVED','Person', 'Four', '2023-09-29','AP'),
(5,'CRD','1.0','500','A1234AE','MDI',1,1,1,'APPROVED','Person', 'Five', '2023-09-29','AP'),
(6,'CRD','1.0','600',null,'ABC',1,1,1,'SUBMITTED','Person', 'Six', '2023-09-29','AP'),
(7,'CRD','1.0','B12345','A1234BC','ABC',1,1,1,'SUBMITTED','Person', 'Seven', null,'AP'),
(8,'CRD','1.0','C12345','B1234BC','ABC',1,1,1,'SUBMITTED','Person', 'Eight', '2024-03-14','AP'),
(9,'CRD','1.0','D12345','C1234BC','ABC',1,1,1,'SUBMITTED','Person', 'Z', '2022-01-01','AP');

