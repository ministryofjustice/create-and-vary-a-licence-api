
CREATE TABLE nomis_time_served_licence (
                                            id SERIAL NOT NULL CONSTRAINT nomis_time_served_licence_pk PRIMARY KEY,
                                            noms_id VARCHAR(7) NOT NULL,
                                            booking_id INTEGER NOT NULL,
                                            reason TEXT NOT NULL,
                                            prison_code VARCHAR(3),
                                            updated_by_ca_id INTEGER NOT NULL REFERENCES staff(id),
                                            date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            date_last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_nomis_time_served_licence ON nomis_time_served_licence(noms_id);
