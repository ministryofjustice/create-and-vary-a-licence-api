insert into licence (
    id,
    noms_id,
    com_staff_id,
    forename,
    surname,
    type_code,
    status_code,
    prison_code,
    prison_description
) values
(1,'A1234AA',125,'Alan','Alda','AP','SUBMITTED', 'MDI','Moorland HMP'),
(2,'B1234BB',126,'Bob','Bobson','AP','SUBMITTED', 'MDI', 'Moorland HMP'),
(3,'C1234CC',125,'Cath','Cookson','AP','ACTIVE', 'LEI', 'Leeds HMP'),
(4,'C1234DD',126,'Kate','Harcourt','AP','APPROVED', 'BMI', 'Birmingham HMP'),
(5,'C1234EE',127,'Mark','Royle','AP','IN_PROGRESS', 'BMI', 'Birmingham HMP'),
(6,'C1234FF',127,'Harold','Biggs','AP','REJECTED', 'LEI', 'Leeds HMP');