CREATE TABLE time_served_probation_confirm_contact (
	   id SERIAL NOT NULL constraint time_served_probation_confirm_contact_key PRIMARY KEY,
	   licence_id BIGINT NOT NULL UNIQUE,
	   confirmed_by_username varchar(100) NOT NULL,
	   contact_status VARCHAR(50) NOT NULL,
	   communication_methods TEXT NOT NULL,
	   other_detail TEXT,
	   date_created TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
	   date_last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_time_served_probation_confirm_contact_licence_id
	ON time_served_probation_confirm_contact(licence_id);

ALTER TABLE time_served_probation_confirm_contact
	ADD CONSTRAINT check_contact_status
		CHECK (contact_status IN ('ALREADY_CONTACTED', 'WILL_CONTACT_SOON', 'CANNOT_CONTACT'));