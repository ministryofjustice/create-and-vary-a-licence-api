CREATE TABLE hdc_weekly_curfew_times (
                                         licence_id INTEGER NOT NULL,
                                         curfew_time_id INTEGER NOT NULL,
                                         PRIMARY KEY (licence_id, curfew_time_id),

                                         CONSTRAINT hdc_weekly_curfew_times_licence_fk
                                             FOREIGN KEY (licence_id) REFERENCES licence(id),

                                         CONSTRAINT hdc_weekly_curfew_times_curfew_fk
                                             FOREIGN KEY (curfew_time_id) REFERENCES curfew_times(id)
);

CREATE TABLE hdc_first_night_curfew_times (
                                              licence_id INTEGER NOT NULL PRIMARY KEY,
                                              curfew_time_id INTEGER NOT NULL,

                                              CONSTRAINT hdc_first_night_curfew_times_licence_fk
                                                  FOREIGN KEY (licence_id) REFERENCES licence(id),

                                              CONSTRAINT hdc_first_night_curfew_times_curfew_fk
                                                  FOREIGN KEY (curfew_time_id) REFERENCES curfew_times(id)
);
