insert into hdc_curfew_address (licence_id, first_line, town_or_city, postcode, reference, source, accommodation_type)
select
    id,
    '1 Some Address',
    'Some Town',
    'AB1 2CD',
    gen_random_uuid()::text,
    'MANUAL',
    'RESIDENTIAL'
from licence
where kind in ('HDC', 'HDC_VARIATION') and status_code in ('IN_PROGRESS', 'SUBMITTED', 'VARIATION_APPROVED');
