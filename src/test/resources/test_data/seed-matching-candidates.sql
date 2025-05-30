insert into licence (
    id,
    kind,
    version,
    noms_id,
    forename,
    surname,
    type_code,
    status_code,
    prison_code,
    prison_description,
    conditional_release_date,
    responsible_com_id,
    created_by_com_id,
    date_created
) values
(1,'CRD','1.0','A1234AA','Person','One','AP','SUBMITTED', 'MDI','Moorland HMP', '2031-04-28', 1, 1, '2022-07-27 15:00:00'),
(2,'CRD','1.0','B1234BB','Person','Two','AP','SUBMITTED', 'MDI', 'Moorland HMP', '2032-04-28', 1, 1, '2022-07-27 15:00:00'),
(3,'CRD','1.0','C1234CC','Person','Three','AP','ACTIVE', 'LEI', 'Leeds HMP', '2033-04-28', 2, 1,'2022-07-27 15:00:00'),
(4,'CRD','1.0','C1234DD','Person','Four','AP','APPROVED', 'BMI', 'Birmingham HMP', '2034-04-28', 3, 1, '2022-07-27 15:00:00'),
(5,'CRD','1.0','C1234EE','Person','Five','AP','IN_PROGRESS', 'BMI', 'Birmingham HMP', '2035-04-28', 2, 1, '2022-07-27 15:00:00'),
(6,'CRD','1.0','C1234FF','Person','Six','AP','REJECTED', 'LEI', 'Leeds HMP', '2036-04-28', 3, 1, '2022-07-27 15:00:00');
