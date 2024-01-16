
CREATE UNIQUE INDEX prevent_duplicate_in_progress_licence_idx ON licence (booking_id)
    WHERE licence.status_code = 'IN_PROGRESS';
