CREATE TABLE hdc_curfew_address(
                                 id SERIAL NOT NULL CONSTRAINT hdc_curfew_address_pk PRIMARY KEY,
                                 licence_id INTEGER REFERENCES licence(id),
                                 address_line1 VARCHAR(200),
                                 address_line2 VARCHAR(200),
                                 town_or_city VARCHAR(200),
                                 county VARCHAR(200),
                                 postcode VARCHAR(10)
);

CREATE INDEX idx_hdc_curfew_address_licence_id ON hdc_curfew_address(licence_id);
