insert into hdc_curfew_address (licence_id, first_line, town_or_city, postcode, reference, source, accommodation_type)
values (
    (
        select id from licence
        where kind = 'HDC' and status_code = 'SUBMITTED' and crn = 'X12348' and noms_id = 'AB1234E'
    ),
    '1 Some Address',
    'Some Town',
    'AB1 2CD',
    '123e4567-e89b-12d3-a456-426614174000',
    'MANUAL',
    'RESIDENTIAL'
);
