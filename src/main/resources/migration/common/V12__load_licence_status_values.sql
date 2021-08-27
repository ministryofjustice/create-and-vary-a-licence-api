-- When an ACTIVE licence exists and a new one is created the old one is set to SUPERSEDED when the new
-- one is approved to become ACTIVE.
insert into licence_status (status_code, description) values ('CREATED', 'Created');
insert into licence_status (status_code, description) values ('REVIEW', 'Awaiting review');
insert into licence_status (status_code, description) values ('ACTIVE', 'Active');
insert into licence_status (status_code, description) values ('SUPERSEDED', 'Superseded');
