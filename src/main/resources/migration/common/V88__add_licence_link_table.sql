CREATE TABLE licence_link (
		id BIGSERIAL PRIMARY KEY,
		from_licence_id INTEGER NOT NULL REFERENCES licence(id) ON DELETE CASCADE,
		to_licence_id INTEGER NOT NULL REFERENCES licence(id) ON DELETE CASCADE
		link_type VARCHAR(50) NOT NULL
);

CREATE UNIQUE INDEX ux_licence_link_from_type
		ON licence_link (from_licence_id, link_type);

CREATE UNIQUE INDEX ux_licence_link_to_type
		ON licence_link (to_licence_id, link_type);
