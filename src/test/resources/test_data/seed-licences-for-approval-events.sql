insert into licence (
    id, kind, type_code, version, status_code, noms_id, crn,
    forename, surname, prison_code, prison_description, licence_start_date,
    responsible_com_id, created_by_com_id, variation_of_id
) values
    (1, 'CRD', 'AP', '1.0', 'SUBMITTED', 'A1234AA', 'CRN1',
     'Person', 'One', 'MDI', 'Moorland (HMP)', current_date + 1, 1, 1, null),
    (2, 'PRRD', 'AP', '1.0', 'SUBMITTED', 'A1234AB', 'CRN2',
     'Person', 'Two', 'MDI', 'Moorland (HMP)', current_date + 1, 1, 1, null),
    (3, 'HDC', 'AP', '1.0', 'SUBMITTED', 'A1234AC', 'CRN3',
     'Person', 'Three', 'MDI', 'Moorland (HMP)', current_date + 1, 1, 1, null),
    (4, 'HARD_STOP', 'AP', '1.0', 'SUBMITTED', 'A1234AD', 'CRN4',
     'Person', 'Four', 'MDI', 'Moorland (HMP)', current_date + 1, 1, null, null),
    (5, 'TIME_SERVED', 'AP', '1.0', 'SUBMITTED', 'A1234AE', 'CRN5',
     'Person', 'Five', 'MDI', 'Moorland (HMP)', current_date + 1, 1, null, null),
    (6, 'VARIATION', 'AP', '2.0', 'VARIATION_SUBMITTED', 'A1234AA', 'CRN1',
     'Person', 'One', 'MDI', 'Moorland (HMP)', current_date + 1, 1, 1, 1),
    (7, 'HDC_VARIATION', 'AP', '2.0', 'VARIATION_SUBMITTED', 'A1234AC', 'CRN3',
     'Person', 'Three', 'MDI', 'Moorland (HMP)', current_date + 1, 1, 1, 3);
