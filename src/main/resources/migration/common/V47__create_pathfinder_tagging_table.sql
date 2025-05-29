CREATE TABLE electronic_monitoring_provider (
                                id SERIAL NOT NULL CONSTRAINT electronic_monitoring_provider_pk PRIMARY KEY,
                                licence_id INTEGER NOT NULL CONSTRAINT electronic_monitoring_provider_licence_fk REFERENCES licence(id) UNIQUE,
                                is_to_be_tagged_for_programme BOOLEAN,
                                programme_name VARCHAR(100),
                                date_created TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                date_last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_electronic_monitoring_provider_licence_id ON electronic_monitoring_provider(licence_id);
