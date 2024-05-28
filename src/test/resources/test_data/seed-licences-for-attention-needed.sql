insert into licence (id,
                     kind,
                     version,
                     responsible_com_id,
                     created_by_com_id,
                     type_code,
                     noms_id,
                     status_code,
                     booking_id,
                     actual_release_date,
                     conditional_release_date,
                     licence_start_date,
                     licence_version,
                     version_of_id)
values (1, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'APPROVED', 456, null, null, null, '1.0', null),
       (2, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'APPROVED', 789, current_date - 1, current_date - 1, current_date - 1,
        '1.0', null),
       (3, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'APPROVED', 432, current_date, current_date, current_date, '1.0', null),
       (4, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'IN_PROGRESS', 456, current_date, current_date, current_date, '1.1', 1),
       (5, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'SUBMITTED', 432, current_date, current_date, current_date, '1.1', 3),
       (6, 'CRD', '1.0', 1, 1, 'AP', 'A1234AA', 'TIMED_OUT', 521, null, null, null, '1.0', null),
       (7, 'HARD_STOP', '1.0', 1, 1, 'AP', 'A1234AA', 'APPROVED', 521, current_date, current_date, current_date, '1.0',
        null)
;