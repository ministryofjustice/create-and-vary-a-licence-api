insert into licence (id,
                     kind,
                     version,
                     noms_id,
                     responsible_com_id,
                     created_by_com_id,
                     type_code,
                     status_code,
                     variation_of_id,
                     probation_team_code)
values (1, 'CRD', '1.0', 'A1234AA', 1, 4, 'AP', 'INACTIVE', null, 'team 1'),
       (2, 'CRD', '1.0', 'A1234AA', 2, 5, 'AP', 'ACTIVE', 1, 'team 2');
