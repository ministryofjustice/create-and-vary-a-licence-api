CREATE UNIQUE INDEX prevent_duplicate_licence_idx ON licence (noms_id)
    WHERE licence.status_code in ('IN_PROGRESS', 'SUBMITTED', 'VARIATION_IN_PROGRESS', 'VARIATION_SUBMITTED');
