ALTER TABLE licence ADD COLUMN version_of_id integer references licence(id);
CREATE INDEX idx_licence_version_of_id ON licence(version_of_id);
