INSERT INTO nomis_time_served_licence (
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

