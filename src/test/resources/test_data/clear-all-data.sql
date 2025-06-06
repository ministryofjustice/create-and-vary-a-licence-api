DELETE from electronic_monitoring_provider;
DELETE from standard_condition;
DELETE from additional_condition_data;
DELETE from additional_condition_upload_detail;
DELETE from additional_condition_upload_summary;
DELETE from additional_condition;
DELETE from bespoke_condition;
DELETE from hdc_curfew_address;
DELETE from hdc_curfew_times;
DELETE from licence;
DELETE from staff;
DELETE from audit_event;
DELETE from omu_contact;

-- Reset auto id in all tables with auto id
ALTER SEQUENCE standard_condition_id_seq RESTART WITH 1;
ALTER SEQUENCE additional_condition_data_id_seq RESTART WITH 1;
ALTER SEQUENCE additional_condition_upload_detail_id_seq RESTART WITH 1;
ALTER SEQUENCE additional_condition_upload_summary_id_seq RESTART WITH 1;
ALTER SEQUENCE additional_condition_id_seq RESTART WITH 1;
ALTER SEQUENCE bespoke_condition_id_seq RESTART WITH 1;
ALTER SEQUENCE hdc_curfew_address_id_seq RESTART WITH 1;
ALTER SEQUENCE hdc_curfew_times_id_seq RESTART WITH 1;
ALTER SEQUENCE licence_id_seq RESTART WITH 1;
ALTER SEQUENCE community_offender_manager_id_seq RESTART WITH 1;
ALTER SEQUENCE audit_event_id_seq RESTART WITH 1;
ALTER SEQUENCE omu_contact_id_seq RESTART WITH 1;
