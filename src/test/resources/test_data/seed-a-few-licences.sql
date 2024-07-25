insert into licence (id,
                     kind,
                     version,
                     noms_id,
                     responsible_com_id,
                     created_by_com_id,
                     type_code,
                     status_code)
values (1, 'CRD', '1.0', 'A1234AA', 1, 4, 'AP', 'APPROVED'),
       (2, 'CRD', '1.0', 'A1234AA', 2, 5, 'AP', 'APPROVED'),
       (4, 'CRD', '1.0', 'A1234AA', 2, 6, 'AP', 'APPROVED'),
       (5, 'CRD', '1.0', 'A1234AA', 2, 7, 'AP', 'APPROVED'),
       (3, 'CRD', '1.0', 'A1234AA', 3, 8, 'AP', 'APPROVED');
