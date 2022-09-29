insert into audit_event (id, licence_id, username, full_name, event_type, summary, detail)
values
  (1, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary1', 'Detail1'),
  (2, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary2', 'Detail2'),
  (3, 1, 'USER', 'Bob Smith', 'USER_EVENT', 'Summary3', 'Detail3');


INSERT INTO audit_event (
id,
licence_id,
event_time,
username,
full_name,
event_type,
summary,
detail)
VALUES
 (4,1,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for Joe Bloggs','ID 1 type AP status APPROVED version 2.0'),
 (5,2,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for Helen Jones','ID 2 type AP status SUBMITTED version 2.0'),
 (6,3,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for Jim Smith','ID 3 type AP status APPROVED version 2.0'),
 (7,2,'2022-09-08 10:24:22.000','CVL_OMU','Omu User','USER_EVENT','Licence viewed for Prison A','ID 4 type AP status SUBMITTED version 2.0');
