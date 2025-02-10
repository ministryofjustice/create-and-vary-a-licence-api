insert into licence (id,
                     kind,
                     version,
                     responsible_com_id,
                     created_by_com_id,
                     type_code,
                     noms_id,
                     status_code,
                     booking_id,
                     licence_start_date,
                     licence_version,
                     version_of_id)
values (1, 'CRD', '1.0', 1, 1, 'AP', 'G7285UT', 'APPROVED', 456, current_date, '1.0', null),
       (2, 'CRD', '1.0', 1, 1, 'AP', 'G5613GT', 'APPROVED', 789, current_date, '1.0', null),
       (3, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'APPROVED', 432, current_date, '1.0', null),
       (4, 'CRD', '1.0', 1, 1, 'AP', 'G7285UT', 'IN_PROGRESS', 456, current_date, '1.1', 1),
       (5, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'SUBMITTED', 432, current_date, '1.1', 3),
       (6, 'CRD', '1.0', 1, 1, 'AP', 'G7285AA', 'TIMED_OUT', 521, current_date, '1.0', null),
       (7, 'HARD_STOP', '1.0', 1, 1, 'AP', 'G7285AA', 'APPROVED', 521, current_date, '1.0', null),
       (8, 'HDC', '1.0', 1, 1, 'AP', 'G1234BB', 'APPROVED', 123, current_date, '1.0', null)
;
