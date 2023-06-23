insert into audit_event (id, licence_id, username, full_name, event_type, summary, detail, changes)
values
    (1, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary1', 'Detail1', null),
    (2, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary2', 'Detail2', null),
    (3, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary3', 'Detail3', null);


INSERT INTO audit_event (
    id,
    licence_id,
    event_time,
    username,
    full_name,
    event_type,
    summary,
    detail,
    changes)
VALUES
    (4,1,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for jim smith','ID 1 type AP status APPROVED version 2.0', null),
    (5,2,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for harry hope','ID 2 type AP status INACTIVE version 2.0', null),
    (6,3,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for terry towel','ID 3 type AP status APPROVED version 2.0', null),
    (7,2,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for harry hope','ID 2 type AP status SUBMITTED version 2.0', null),
    (8,3,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for terry towel','ID 3 type AP status APPROVED version 2.0', null),
    (9,5,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for prisoner five','ID 5 type AP status SUBMITTED version 2.0', null),
    (10,5,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for prisoner five','ID 5 type AP status APPROVED version 2.0', null),
    (11,6,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for prisoner six','ID 6 type AP status SUBMITTED version 2.0', null),
    (12,6,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for prisoner six','ID 6 type AP status APPROVED version 2.0', null),
    (13,4,'2022-09-10 03:00:00.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for bob pint','ID 4 type AP status ACTIVE version 2.0', null);
