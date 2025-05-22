UPDATE licence_event SET event_type = 'CREATED' where event_type = 'HDC_CREATED';

UPDATE licence_event SET event_type = 'VERSION_CREATED' where event_type = 'HDC_VERSION_CREATED';

UPDATE licence_event SET event_type = 'SUBMITTED' where event_type = 'HDC_SUBMITTED';

UPDATE licence_event SET event_type = 'VARIATION_CREATED' where event_type = 'HDC_VARIATION_CREATED';

UPDATE licence_event SET event_type = 'VARIATION_SUBMITTED' where event_type = 'HDC_VARIATION_SUBMITTED';

UPDATE licence_event SET event_type = 'VARIATION_APPROVED' where event_type = 'HDC_VARIATION_APPROVED';

UPDATE licence_event SET event_type = 'VARIATION_REFERRED' where event_type = 'HDC_VARIATION_REFERRED';
