CREATE TABLE staff_saved_appointment_address (
	 staff_id INTEGER NOT NULL,
	 address_id INTEGER NOT NULL,

     CONSTRAINT pk_staff_saved_address PRIMARY KEY (staff_id, address_id), -- Unique pair

	 CONSTRAINT fk_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
	 CONSTRAINT fk_address FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE
);

CREATE INDEX idx_staff_saved_address_staff_id ON staff_saved_appointment_address(staff_id);
CREATE INDEX idx_staff_saved_address_address_id ON staff_saved_appointment_address(address_id);
