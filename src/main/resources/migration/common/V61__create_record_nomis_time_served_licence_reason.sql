CREATE TABLE record_nomis_time_served_licence_reason (
                                            id SERIAL NOT NULL CONSTRAINT record_nomis_time_served_licence_reason PRIMARY KEY,
                                            noms_id VARCHAR(7) NOT NULL,
                                            booking_id INTEGER NOT NULL,
                                            reason TEXT NOT NULL,
                                            prison_code VARCHAR(3),
                                            updated_by_ca_id INTEGER NOT NULL REFERENCES staff(id),
                                            date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                            date_last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_record_nomis_time_served_licence_reason ON record_nomis_time_served_licence_reason(noms_id);
