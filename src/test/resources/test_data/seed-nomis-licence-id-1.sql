-- INSERT INTO staff(id, kind, staff_identifier, username, email, first_name, last_name)
-- VALUES
--     (1, 'PRISON_USER', 9000, 'Staff1', 'prisonClient@prison.gov.uk', 'Prison', 'User');

INSERT INTO nomis_time_served_licences (
    noms_id,
    booking_id,
    reason,
    prison_code,
    updated_by_ca_id,
    date_created,
    date_last_updated
)
VALUES (
           'A1234AA',
           123456,
           'Time served licence created for conditional release',
           'MDI',
           1,
           CURRENT_TIMESTAMP,
           CURRENT_TIMESTAMP
       );

