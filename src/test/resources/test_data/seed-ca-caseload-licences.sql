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
        'AP','2022-03-25'),
       (4, 'CRD', '1.0', '400', 'A1234AD', 'MDI', 1, 1, 1, 'TIMED_OUT', 'Person', 'Four', '2023-09-29',
        'AP','2022-04-25'),
       (5, 'CRD', '1.0', '500', 'A1234AE', 'BAI', 1, 1, 1, 'ACTIVE', 'Person', 'Five', '2023-09-29',
        'AP','2022-05-25'),
       (6, 'CRD', '1.0', '600', 'A1234AF', 'BAI', 1, 1, 1, 'VARIATION_APPROVED', 'Person', 'Six','2023-09-29',
        'AP','2022-06-25'),
       (7, 'CRD', '1.0', '700', 'A1234BC', 'ABC', 1, 1, 1, 'VARIATION_IN_PROGRESS', 'Person', 'Seven', null,
        'AP','2022-07-25'),
       (8, 'CRD', '1.0', '800', 'B1234BC', 'ABC', 1, 1, 1, 'VARIATION_SUBMITTED', 'Person', 'Eight', '2024-03-14',
        'AP','2022-08-25');


INSERT INTO record_nomis_time_served_licence_reason (
    noms_id,
    booking_id,
    reason,
    prison_code,
    updated_by_ca_id,
    date_created,
    date_last_updated
)
VALUES (
           'A1234AF',
           '123',
           'Time served licence created for conditional release',
           'MDI',
           1,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );