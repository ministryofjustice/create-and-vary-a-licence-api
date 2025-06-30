
CREATE TABLE address (
	 id 			SERIAL  NOT NULL constraint address_pk PRIMARY KEY,
	 uprn			TEXT    NULL,
	 reference 		TEXT    NOT NULL,
	 first_line     TEXT    NOT NULL,  -- First line of address
	 second_line    TEXT    NULL,      -- Optional second line
	 town_or_city   TEXT    NOT NULL,  -- Town or city
	 county         TEXT    NULL,  	   -- County
	 postcode       TEXT    NOT NULL,  -- Postcode (e.g.CF23 3HS)
     source 		TEXT    NOT NULL DEFAULT 'MANUAL',
     created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	 last_updated_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
	 CONSTRAINT chk_address_source_valid CHECK (source IN ('MANUAL', 'OS_PLACES'))
);

-- index
ALTER TABLE address ADD CONSTRAINT unique_reference UNIQUE (reference);
CREATE INDEX idx_address_postcode ON address(postcode);
CREATE INDEX idx_address_town ON address(town_or_city);

-- Step 1: Create the trigger function
CREATE OR REPLACE FUNCTION address_update_last_updated_timestamp()
RETURNS TRIGGER AS $$
BEGIN
  NEW.last_updated_timestamp := CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Step 2: Create the trigger
CREATE TRIGGER set_address_last_updated_timestamp
	BEFORE UPDATE ON address
	FOR EACH ROW
	EXECUTE FUNCTION address_update_last_updated_timestamp();

CREATE TABLE licence_appointment_address (
	 licence_id INTEGER NOT NULL UNIQUE,   -- One licence only
	 address_id INTEGER NOT NULL,   -- Address can be used by more than one licence

	 CONSTRAINT fk_licence FOREIGN KEY (licence_id) REFERENCES licence(id) ON DELETE CASCADE,
	 CONSTRAINT fk_address FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE,

	 CONSTRAINT licence_appointment_address_pk PRIMARY KEY (licence_id, address_id)
);