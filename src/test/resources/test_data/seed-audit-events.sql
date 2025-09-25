insert into audit_event (id, licence_id, username, full_name, event_type, summary, detail, changes, event_time)
values (1, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary1', 'Detail1', null, CURRENT_TIMESTAMP),
       (2, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary2', 'Detail2', null, CURRENT_TIMESTAMP),
       (3, 1, 'USER', 'Test User', 'USER_EVENT', 'Summary3', 'Detail3', null, CURRENT_TIMESTAMP);


INSERT INTO audit_event (id,
                         licence_id,
                         event_time,
                         username,
                         full_name,
                         event_type,
                         summary,
                         detail,
                         changes)
VALUES (4, 1, '2022-09-08 10:24:22.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person One',
        'ID 1 type AP status APPROVED version 2.0', null),
       (5, 2, '2022-09-08 10:24:22.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Two',
        'ID 2 type AP status INACTIVE version 2.0', null),
       (6, 3, '2022-09-08 10:24:22.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Three',
        'ID 3 type AP status APPROVED version 2.0', null),
       (7, 2, '2022-09-08 10:24:22.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Two',
        'ID 2 type AP status SUBMITTED version 2.0', null),
       (8, 3, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Three',
        'ID 3 type AP status APPROVED version 2.0', null),
       (9, 5, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Five',
        'ID 5 type AP status SUBMITTED version 2.0', null),
       (10, 5, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Five',
        'ID 5 type AP status APPROVED version 2.0', null),
       (11, 6, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Six',
        'ID 6 type AP status SUBMITTED version 2.0', null),
       (12, 6, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Six',
        'ID 6 type AP status APPROVED version 2.0', null),
       (13, 4, '2022-09-10 03:00:00.000', 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Four',
        'ID 4 type AP status ACTIVE version 2.0', null),
       (14, 4, CURRENT_TIMESTAMP, 'CVL_OMU', 'Omu User', 'USER_EVENT', 'Licence viewed for Person Seven',
        'ID 4 type AP status IN_PROGRESS version 3.0', null);

