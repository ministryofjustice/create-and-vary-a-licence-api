INSERT INTO staff(id, kind, staff_identifier, username, email, first_name, last_name)
VALUES
    (15, 'PRISON_USER', 9000, 'Staff1', 'prisonClient@prison.gov.uk', 'Prison', 'User');

INSERT INTO staff(id, kind, staff_identifier, username, email, first_name, last_name)
VALUES
    (16, 'COMMUNITY_OFFENDER_MANAGER', 9001, 'Staff2', 'testClient@prison.gov.uk', 'Test', 'Client');

INSERT INTO address (id, reference, first_line, second_line, town_or_city, county, postcode, source)
VALUES
    (1,'REF-123456', '123 Test Street', 'Apt 4B', 'Testville', 'Testshire', 'TE5 7AA','MANUAL');

INSERT INTO address (id, uprn, reference, first_line, second_line, town_or_city, county, postcode, source)
VALUES
    (2,'UPRN','REF-123457', '1234 Test Street', 'Apt 4BC', 'Testville1', 'Testshire1', 'TE5 7AB','OS_PLACES');

INSERT INTO staff_saved_appointment_address (staff_id, address_id)
VALUES
    (15, 1),
    (15, 2);
