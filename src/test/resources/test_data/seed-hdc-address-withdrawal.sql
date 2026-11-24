UPDATE hdc_application
SET proposed_accommodation_type = 'RESIDENTIAL'
WHERE licence_id IN (
    SELECT id FROM licence WHERE kind = 'HDC' AND noms_id = 'A1234AA'
);

INSERT INTO hdc_curfew_address (licence_id, reference, first_line, town_or_city, postcode, source, accommodation_type)
SELECT id, CONCAT('withdrawal-', id), '123 Test Street', 'Test Town', 'TE1 1ST', 'HDC_CREATION', 'RESIDENTIAL'
FROM licence
WHERE kind = 'HDC' AND noms_id = 'A1234AA';
