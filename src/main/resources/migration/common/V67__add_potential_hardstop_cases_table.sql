CREATE TABLE potential_hardstop_case
(
    id                SERIAL  NOT NULL
        CONSTRAINT potential_hardstop_case_pk PRIMARY KEY,
    licence_id        INTEGER NOT NULL,
    status            VARCHAR(255),
    date_created      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    date_last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT hardstop_case_fk_licence FOREIGN KEY (licence_id) REFERENCES licence (id) ON DELETE CASCADE
);

CREATE INDEX idx_potential_hardstop_cases_licence ON potential_hardstop_case (licence_id);
