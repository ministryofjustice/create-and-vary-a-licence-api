update licence set version = '4.0' where id = 1;

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, expanded_condition_text, condition_type)
values (10, 1, '4.0', 'Curfew arrangement', '0a370862-5426-49c1-b6d4-3d074d78a81a', 1,
        'One curfew', 'One curfew condition text', 'AP'),
       (11, 1, '4.0', 'Curfew arrangement', '0a370862-5426-49c1-b6d4-3d074d78a81a', 2,
        'Two curfews', 'Two curfews condition text', 'AP'),
       (12, 1, '4.0', 'Curfew arrangement', '0a370862-5426-49c1-b6d4-3d074d78a81a', 3,
        'Three curfews', 'Three curfews condition text', 'AP');

insert into additional_condition_data (additional_condition_id, data_sequence, data_field, data_value)
values (10, 1, 'oneCurfewStart', '01:00 am'),
       (10, 2, 'oneCurfewEnd', '02:00 pm'),
       (11, 1, 'twoCurfewStart', '03:15 am'),
       (11, 2, 'twoCurfewEnd', '04:45 pm'),
       (11, 3, 'twoCurfewStart2', '05:30 am'),
       (11, 4, 'twoCurfewEnd2', '06:45 pm'),
       (12, 1, 'threeCurfewStart', '07:10 am'),
       (12, 2, 'threeCurfewEnd', '08:20 am'),
       (12, 3, 'threeCurfewStart2', '09:30 am'),
       (12, 4, 'threeCurfewEnd2', '10:40 am'),
       (12, 5, 'threeCurfewStart3', '11:50 am'),
       (12, 6, 'threeCurfewEnd3', '12:55 pm');
