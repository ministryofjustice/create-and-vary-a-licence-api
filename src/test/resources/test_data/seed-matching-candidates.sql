insert into licence (
    id,
    noms_id,
    com_staff_id,
    forename,
    surname,
    type_code,
    status_code,
    prison_code,
    prison_description,
    conditional_release_date
) values
(1,'A1234AA',125,'Alan','Alda','AP','SUBMITTED', 'MDI','Moorland HMP', '2031-04-28'),
(2,'B1234BB',126,'Bob','Bobson','AP','SUBMITTED', 'MDI', 'Moorland HMP', '2032-04-28'),
(3,'C1234CC',125,'Cath','Cookson','AP','ACTIVE', 'LEI', 'Leeds HMP', '2033-04-28'),
(4,'C1234DD',126,'Kate','Harcourt','AP','APPROVED', 'BMI', 'Birmingham HMP', '2034-04-28'),
(5,'C1234EE',127,'Mark','Royle','AP','IN_PROGRESS', 'BMI', 'Birmingham HMP', '2035-04-28'),
(6,'C1234FF',127,'Harold','Biggs','AP','REJECTED', 'LEI', 'Leeds HMP', '2036-04-28');
