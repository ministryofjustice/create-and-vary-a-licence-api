update licence set kind = 'HDC' where id = 3;
update licence set kind = 'HDC_VARIATION' where id = 6;

insert into hdc_application (licence_id, is_postponed, proposed_accommodation_type)
values (3, false, 'RESIDENTIAL'),
       (6, false, 'RESIDENTIAL');
