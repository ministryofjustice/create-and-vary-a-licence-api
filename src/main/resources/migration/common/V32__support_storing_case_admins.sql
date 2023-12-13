ALTER TABLE community_offender_manager ADD COLUMN kind varchar(30);
UPDATE community_offender_manager SET kind = 'COMMUNITY_OFFENDER_MANAGER';
ALTER TABLE community_offender_manager ALTER COLUMN kind SET NOT NULL;
ALTER TABLE community_offender_manager DROP CONSTRAINT community_offender_manager_staff_identifier_key;

ALTER TABLE community_offender_manager RENAME TO staff;

ALTER TABLE licence ALTER COLUMN created_by_com_id DROP NOT NULL;

ALTER TABLE licence ADD COLUMN created_by_ca_id INTEGER NULL REFERENCES staff(id);
CREATE INDEX idx_licence_created_by_ca_id ON licence(created_by_ca_id);

ALTER TABLE licence ADD COLUMN submitted_by_ca_id INTEGER NULL REFERENCES staff(id);
CREATE INDEX idx_licence_submitted_by_ca_id ON licence(submitted_by_ca_id);

ALTER TABLE licence ADD COLUMN review_date timestamp with time zone;

ALTER TABLE licence ADD COLUMN substitute_of_id INTEGER NULL REFERENCES licence(id);
CREATE INDEX idx_licence_substitute_of_id ON licence(substitute_of_id);

CREATE INDEX idx_licence_responsible_com_id ON licence(responsible_com_id);
CREATE INDEX idx_licence_created_by_com_id ON licence(created_by_com_id);
CREATE INDEX idx_licence_submitted_by_com_id ON licence(submitted_by_com_id);
