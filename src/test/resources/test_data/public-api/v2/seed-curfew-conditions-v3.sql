update licence set version = '3.0' where id = 1;

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, expanded_condition_text, condition_type)
values (10, 1, '3.0', 'Curfew arrangement', '0a370862-5426-49c1-b6d4-3d074d78a81a', 1,
        'Curfew condition', 'Curfew condition text', 'AP');

insert into additional_condition_data (additional_condition_id, data_sequence, data_field, data_value)
values (10, 1, 'curfewStart', '07:00 pm'),
       (10, 2, 'curfewEnd', '7:00 PM'),
       (10, 3, 'curfewStart2', '8:30 AM'),
       (10, 4, 'curfewEnd2', '09:45 pm'),
       (10, 5, 'curfewStart3', '06:15 am'),
       (10, 6, 'curfewEnd3', '10:30 PM');
