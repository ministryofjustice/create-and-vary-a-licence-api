ALTER TABLE licence_status ALTER COLUMN status_code TYPE varchar (40);
ALTER TABLE licence ALTER COLUMN status_code TYPE varchar (40);

INSERT INTO licence_status (status_code, description) VALUES ('VARIATION_IN_PROGRESS', 'Variation in progress');
INSERT INTO licence_status (status_code, description) VALUES ('VARIATION_SUBMITTED', 'Variation submitted');
