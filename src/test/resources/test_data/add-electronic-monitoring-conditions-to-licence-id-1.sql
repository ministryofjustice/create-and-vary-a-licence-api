update licence set version = '4.0', type_code = 'AP_PSS' where id = 1;

insert into additional_condition (id, licence_id, condition_version, condition_category, condition_code,
                                  condition_sequence, condition_text, expanded_condition_text, condition_type)
values (1, 1, '4.0', 'Electronic monitoring', 'fd129172-bdd3-4d97-a4a0-efd7b47a49d4',
        1, 'Electronic monitoring condition', 'Electronic monitoring until Sunday 16 January 2028', 'AP'),
       (2, 1, '4.0', 'Electronic monitoring', '524f2fd6-ad53-47dd-8edc-2161d3dd2ed4',
        2, 'Location monitoring condition', 'Location monitoring until Sunday 16 January 2028', 'AP'),
       (3, 1, '4.0', 'Electronic monitoring', '2F8A5418-C6E4-4F32-9E58-64B23550E504',
        3, 'Alcohol monitoring condition', 'Alcohol monitoring until Sunday 16 January 2028', 'AP'),
       (4, 1, '4.0', 'Electronic monitoring', 'fd129172-bdd3-4d97-a4a0-efd7b47a49d4',
        1, 'Electronic monitoring condition', 'Electronic monitoring without an end date', 'PSS'),
       (5, 1, '4.0', 'Electronic monitoring', '524f2fd6-ad53-47dd-8edc-2161d3dd2ed4',
        2, 'Location monitoring condition', 'Location monitoring without an end date', 'PSS'),
       (6, 1, '4.0', 'Electronic monitoring', '2F8A5418-C6E4-4F32-9E58-64B23550E504',
        3, 'Alcohol monitoring condition', 'Alcohol monitoring without an end date', 'PSS');

insert into additional_condition_data (additional_condition_id, data_sequence, data_field, data_value)
values (1, 1, 'electronicMonitoringTypes', 'curfew'),
       (1, 2, 'endDate', 'Sunday 16 January 2028'),
       (2, 1, 'endDate', 'Sunday 16 January 2028'),
       (3, 1, 'endDate', 'Sunday 16 January 2028'),
       (4, 1, 'electronicMonitoringTypes', 'curfew'),
       (5, 1, 'endDate', null),
       (6, 1, 'endDate', ' ');
