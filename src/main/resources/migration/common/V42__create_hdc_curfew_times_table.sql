CREATE TABLE hdc_curfew_times(
                            id serial NOT NULL constraint hdc_curfew_times_pk PRIMARY KEY,
                            licence_id integer references licence(id),
                            from_day varchar(9),
                            from_time time,
                            until_day varchar(9),
                            until_time time,
                            curfew_times_sequence integer,  -- the sequence of the data for these curfew times - starting at 1
                            created_timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hdc_curfew_times_licence_id ON hdc_curfew_times(licence_id);
