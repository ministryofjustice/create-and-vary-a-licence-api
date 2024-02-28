ALTER TABLE licence
    ADD COLUMN updated_by_id INTEGER NULL REFERENCES staff(id);
CREATE INDEX idx_licence_updated_by_id ON licence (updated_by_id);

UPDATE licence l
SET updated_by_id = s.id FROM staff s
WHERE UPPER (l.updated_by_username) = UPPER (s.username);