-- Fix incorrect staff_saved_appointment_address join table name to saved_contact_addresses
ALTER TABLE staff_saved_appointment_address
	RENAME TO saved_contact_addresses;
