update hdc_application
set proposed_accommodation_type = null
where licence_id = (
    select id from licence
    where kind = 'HDC' and status_code = 'SUBMITTED' and crn = 'X12348' and noms_id = 'AB1234E'
);
