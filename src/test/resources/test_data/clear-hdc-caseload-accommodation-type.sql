update hdc_application
set proposed_accommodation_type = null
where licence_id in (
    select id from licence
    where kind in ('HDC', 'HDC_VARIATION') and status_code in ('IN_PROGRESS', 'SUBMITTED', 'VARIATION_APPROVED')
);
