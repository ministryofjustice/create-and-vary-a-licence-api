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
values (1, 'CRD', '1.0', 1, 1, 'AP', 'G7285UT', 'IN_PROGRESS', 456, current_date - 1, '1.0', null),
       (2, 'CRD', '1.0', 1, 1, 'AP', 'G5613GT', 'IN_PROGRESS', 789, current_date + 1, '1.0', null),
       (3, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'ACTIVE', 432, current_date + 1, '1.0', null),
       (4, 'CRD', '1.0', 1, 1, 'AP', 'G7285UT', 'IN_PROGRESS', 457, current_date + 1, '1.1', 1),
       (5, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'SUBMITTED', 432, current_date + 1, '1.1', 3),
       (6, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'SUBMITTED', 432, current_date - 1, '1.1', 3),
       (7, 'HARD_STOP', '1.0', 1, 1, 'AP', 'G4169UO', 'SUBMITTED', 432, current_date - 1, '1.1', 3),
       (8, 'CRD', '1.0', 1, 1, 'AP', 'G4169UO', 'TIMED_OUT', 432, current_date - 1, '1.1', 3);

