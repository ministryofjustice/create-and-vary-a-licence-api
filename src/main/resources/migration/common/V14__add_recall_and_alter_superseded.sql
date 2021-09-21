delete from licence_status where status_code = 'SUPERSEDED';
insert into licence_status (status_code, description) values ('INACTIVE', 'Inactive');
insert into licence_status (status_code, description) values ('RECALLED', 'Recalled');
