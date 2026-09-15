-- Fix incorrect licence_appointment join table name to licence_probation_contact
ALTER TABLE licence_appointment
	RENAME TO licence_probation_contact;

ALTER TABLE licence_probation_contact
	RENAME COLUMN appointment_id TO probation_contact_id;

ALTER TABLE licence_probation_contact
	RENAME CONSTRAINT licence_appointment_pk
	TO licence_probation_contact_pk;

ALTER TABLE licence_probation_contact
	RENAME CONSTRAINT licence_appointment_appointment_fk
	TO licence_probation_contact_probation_contact_fk;

ALTER TABLE licence_probation_contact
	RENAME CONSTRAINT licence_appointment_licence_fk
	TO licence_probation_contact_licence_fk;

ALTER INDEX idx_licence_appointment_appointment_id
    RENAME TO idx_licence_probation_contact_probation_contact_id;

ALTER INDEX idx_licence_appointment_licence_id
    RENAME TO idx_licence_probation_contact_licence_id;
