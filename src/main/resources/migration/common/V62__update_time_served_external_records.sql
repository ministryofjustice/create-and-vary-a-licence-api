-- 1. Drop the existing primary key constraint
ALTER TABLE record_nomis_time_served_licence_reason
    DROP CONSTRAINT record_nomis_time_served_licence_reason_pk;

-- 2. Drop the existing index
DROP INDEX idx_record_nomis_time_served_licence_reason;

-- 3. Rename the table
ALTER TABLE record_nomis_time_served_licence_reason
    RENAME TO time_served_external_records;

-- 4. Add the new primary key constraint
ALTER TABLE time_served_external_records
    ADD CONSTRAINT time_served_external_records_pk PRIMARY KEY (id);

-- 5. Create the new index
CREATE INDEX idx_time_served_external_records
    ON time_served_external_records(noms_id);
