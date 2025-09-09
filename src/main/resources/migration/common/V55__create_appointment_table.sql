-- 1. Create appointment table (without trigger initially)
CREATE TABLE appointment (
							 id SERIAL NOT NULL CONSTRAINT appointment_pk PRIMARY KEY,
							 person_type VARCHAR(50),
							 person VARCHAR(255),
							 time_type VARCHAR(50),
							 time TIMESTAMP WITH TIME ZONE,
							 address_text VARCHAR(255),
							 telephone_contact_number VARCHAR(255),
							 alternative_telephone_contact_number VARCHAR(255),
							 date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
							 date_last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. Appointment > Address join table
CREATE TABLE appointment_address (
									 appointment_id INTEGER NOT NULL CONSTRAINT appointment_address_appointment_fk REFERENCES appointment(id),
									 address_id INTEGER NOT NULL CONSTRAINT appointment_address_address_fk REFERENCES address(id),
									 CONSTRAINT appointment_address_pk PRIMARY KEY (appointment_id, address_id)
);

-- 3. Licence > Appointment join table
CREATE TABLE licence_appointment (
									 licence_id INTEGER NOT NULL CONSTRAINT licence_appointment_licence_fk REFERENCES licence(id),
									 appointment_id INTEGER NOT NULL CONSTRAINT licence_appointment_appointment_fk REFERENCES appointment(id),
									 CONSTRAINT licence_appointment_pk PRIMARY KEY (licence_id, appointment_id)
);

-- 4. Temporary staging table
CREATE TEMP TABLE tmp_appointment (
    id SERIAL PRIMARY KEY,
    licence_id INTEGER NOT NULL,
    person_type VARCHAR(50),
    person VARCHAR(255),
    time_type VARCHAR(50),
    time TIMESTAMP WITH TIME ZONE,
    address_text VARCHAR(255),
    telephone_contact_number VARCHAR(255),
    alternative_telephone_contact_number VARCHAR(255),
    date_created TIMESTAMP WITH TIME ZONE,
    date_last_updated TIMESTAMP WITH TIME ZONE
);

-- 5. Load data from licence into tmp_appointment
INSERT INTO tmp_appointment (
	licence_id,
	person_type,
	person,
	time_type,
	time,
	address_text,
	telephone_contact_number,
	date_created,
	date_last_updated
)
SELECT
	l.id,
	l.appointment_person_type,
	l.appointment_person,
	l.appointment_time_type,
	l.appointment_time,
	l.appointment_address,
	l.appointment_contact,
	l.date_created,
	l.date_last_updated
FROM licence l
WHERE l.appointment_person_type IS NOT NULL
   OR l.appointment_person IS NOT NULL
   OR l.appointment_time_type IS NOT NULL
   OR l.appointment_time IS NOT NULL
   OR l.appointment_address IS NOT NULL
   OR l.appointment_contact IS NOT NULL;

-- 6. Insert into final appointment table from tmp_appointment (fast bulk insert)
INSERT INTO appointment (
	id,
	person_type,
	person,
	time_type,
	time,
	address_text,
	telephone_contact_number,
	date_created,
	date_last_updated
)
SELECT
	id,
	person_type,
	person,
	time_type,
	time,
	address_text,
	telephone_contact_number,
	date_created,
	date_last_updated
FROM tmp_appointment;

-- 7. Ensure appointment id sequence is correct
SELECT setval(pg_get_serial_sequence('appointment', 'id'), MAX(id)) FROM appointment;

-- 8. Populate the new licence_appointment join table
INSERT INTO licence_appointment (licence_id, appointment_id)
	SELECT licence_id, id	FROM tmp_appointment;

-- 9. Migrate appointment addresses
INSERT INTO appointment_address (appointment_id, address_id)
SELECT
	tmp.id,
		laa.address_id
	FROM tmp_appointment tmp
			 JOIN licence_appointment_address laa ON tmp.licence_id = laa.licence_id;

-- 10. Index for performance
CREATE INDEX idx_licence_appointment_licence_id ON licence_appointment(licence_id);
CREATE INDEX idx_licence_appointment_appointment_id ON licence_appointment(appointment_id);

-- 11. Trigger function for automatic date_last_updated
CREATE OR REPLACE FUNCTION appointment_update_last_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.date_last_updated := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 12. Trigger to update date_last_updated before any update
CREATE TRIGGER set_appointment_last_updated_timestamp
	BEFORE UPDATE ON appointment
	FOR EACH ROW
	EXECUTE FUNCTION appointment_update_last_updated_timestamp();

-- 13. Drop migrated appointment columns from licence
ALTER TABLE licence
DROP COLUMN appointment_person_type,
    DROP COLUMN appointment_person,
    DROP COLUMN appointment_time_type,
    DROP COLUMN appointment_time,
    DROP COLUMN appointment_address,
    DROP COLUMN appointment_contact;

-- 14. Drop old join table no longer needed
DROP TABLE licence_appointment_address;
