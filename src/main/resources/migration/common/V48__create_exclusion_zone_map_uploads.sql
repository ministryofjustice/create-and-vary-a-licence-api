CREATE TABLE exclusion_zone_uploads(
    id serial NOT NULL constraint exclusion_zone_uploads_pk PRIMARY KEY,
    licence_id integer references licence(id),
    additional_condition_id integer references additional_condition(id),
    pdf_id uuid,
    thumbnail_id uuid,
    full_image_id uuid,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_exclusion_zone_uploads_licence_id ON exclusion_zone_uploads(licence_id);
CREATE INDEX idx_exclusion_zone_uploads_condition_id ON exclusion_zone_uploads(additional_condition_id);
