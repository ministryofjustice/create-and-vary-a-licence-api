ALTER TABLE appointment
    RENAME COLUMN time_type TO appointment_time_type;

ALTER TABLE appointment
    RENAME COLUMN time TO appointment_time;

ALTER TABLE appointment
    RENAME TO probation_contact;

ALTER TABLE appointment_address
    RENAME TO probation_contact_appointment_address;
