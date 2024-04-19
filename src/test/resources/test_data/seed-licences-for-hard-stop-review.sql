insert into licence (id,
                     forename,
                     surname,
                     kind,
                     version,
                     responsible_com_id,
                     created_by_com_id,
                     type_code,
                     noms_id,
                     crn,
                     status_code,
                     review_date,
                     licence_activated_date)
values (1, 'Test Forename 1', 'Test Surname 1', 'HARD_STOP', '1.0', 1, 1, 'AP', 'G7285UT', 'A123456', 'ACTIVE', null, DATEADD(day, -5, current_date)),
       (2, 'Test Forename 2', 'Test Surname 2', 'HARD_STOP', '1.0', 1, 1, 'AP', 'G5613GT', 'B123456', 'ACTIVE', null, DATEADD(day, -4, current_date)),
       (3, 'Test Forename 3', 'Test Surname 3', 'HARD_STOP', '1.0', 1, 1, 'AP', 'G4169UO', 'C123456', 'ACTIVE', current_date, DATEADD(day, -5, current_date)),
       (4, 'Test Forename 4', 'Test Surname 4', 'HARD_STOP', '1.0', 1, 1, 'AP', 'G7285UT', 'D123456', 'APPROVED', null, null),
       (5, 'Test Forename 5', 'Test Surname 5', 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'E123456', 'SUBMITTED', null, DATEADD(day, -5, current_date)),
       (6, 'Test Forename 6', 'Test Surname 6', 'HARD_STOP', '1.0', 1, 1, 'AP', 'A1234BC', 'F123456', 'IN_PROGRESS', null, DATEADD(day, -6, current_date));


