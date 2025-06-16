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
                     licence_expiry_date,
                     topup_supervision_expiry_date,
                     licence_version,
                     version_of_id,
                     variation_of_id)
values (1,'CRD','1.0', 1, 1, 'AP', 'G7285UT', 'APPROVED', 456, current_date, current_date, CURRENT_DATE + 10, null, '1.0', null, null),
       (2,'CRD','1.0', 1, 1, 'AP', 'G5613GT', 'ACTIVE', 789, CURRENT_DATE - 30, CURRENT_DATE - 31, CURRENT_DATE - 1, null, '1.0', null, null),
       (3,'CRD','1.0', 1, 1, 'AP_PSS', 'G4169UO', 'ACTIVE', 432, CURRENT_DATE - 30, CURRENT_DATE - 31, CURRENT_DATE - 1, CURRENT_DATE + 10, '1.0', null, null),
       (4,'CRD','1.0', 1, 1, 'AP', 'G7285UT', 'IN_PROGRESS', 456, current_date, current_date, CURRENT_DATE + 10, null, '1.0', null, null),
       (5,'CRD','1.0', 1, 1, 'AP', 'G5613GT', 'VARIATION_SUBMITTED', 789, CURRENT_DATE - 30, CURRENT_DATE - 31, CURRENT_DATE - 1, null, '2.0', null, 2),
       (6,'CRD','1.0', 1, 1, 'PSS', 'G4169UO', 'ACTIVE', 432, CURRENT_DATE - 30, CURRENT_DATE - 31, null, CURRENT_DATE - 10, '1.0', null, null),
       (7,'CRD','1.0', 1, 1, 'PSS', 'G4169UO', 'TIMED_OUT', 456, CURRENT_DATE - 30, CURRENT_DATE - 31, null, CURRENT_DATE - 10, '1.0', null, null),
       (8,'HDC','1.0', 1, 1, 'AP', 'A1234BC', 'SUBMITTED', 123, CURRENT_DATE - 30, CURRENT_DATE - 31, null, CURRENT_DATE - 10, '1.0', null, null),
       (9,'HDC','1.0', 1, 1, 'AP', 'B1234CD', 'APPROVED', 123, CURRENT_DATE - 30, CURRENT_DATE - 31, CURRENT_DATE - 10, null, '1.0', null, null)


;
