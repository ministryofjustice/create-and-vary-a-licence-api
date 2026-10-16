insert into licence (id,
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
                     type_code,
                     conditional_release_date)
values (1, 'CRD', '1.0', '100', 'A1234AA', 'BAI', 1, 1, 1, 'APPROVED', 'Person', 'One', '2022-09-29',
        'AP','2022-01-25'),
        (2, 'CRD', '1.0', '200', 'A1234AB', 'BAI', 1, 1, 1, 'SUBMITTED', 'Person', 'Two', '2022-01-01',
        'AP','2022-02-25'),
       (3, 'CRD', '1.0', '300', 'A1234AC', 'BAI', 1, 1, 2, 'IN_PROGRESS', 'Person', 'Three', CURRENT_TIMESTAMP,
        'AP','2022-03-25');


INSERT INTO hdc_postponement (
    licence_id,
    is_postponed
)
VALUES (
        2,
        true
       ),
        (
         3,
         false
        );