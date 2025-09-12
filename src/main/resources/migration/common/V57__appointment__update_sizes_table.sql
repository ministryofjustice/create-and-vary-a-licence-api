-- Updated sizes of several columns in appointment table to match the old requirements
ALTER TABLE appointment
    ALTER COLUMN person_type TYPE VARCHAR(100),
    ALTER COLUMN person TYPE VARCHAR(60),
    ALTER COLUMN time_type TYPE VARCHAR(100),
    ALTER COLUMN address_text TYPE VARCHAR(240),
    ALTER COLUMN telephone_contact_number TYPE VARCHAR(30),
    ALTER COLUMN alternative_telephone_contact_number TYPE VARCHAR(30);

