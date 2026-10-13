CREATE TABLE hdc_postponement (
  id BIGSERIAL PRIMARY KEY,
  licence_id INTEGER NOT NULL REFERENCES licence(id) ON DELETE CASCADE,
  is_postponed BOOLEAN NOT NULL,
  date_created TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
  date_last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX ux_hdc_postponement_licence_id
  ON hdc_postponement (licence_id);
