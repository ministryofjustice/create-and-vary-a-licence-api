CREATE TABLE audit_event(
   id serial NOT NULL constraint audit_event_pk PRIMARY KEY,
   licence_id integer,     -- Can be null when not related to a licence (so no FK relationship)
   event_time timestamp with time zone default CURRENT_TIMESTAMP,
   username varchar(100),  -- Long to cope with potential email address as username, and nullable
   full_name varchar(80),  -- Nullable
   event_type varchar(40) NOT NULL, -- Either SYSTEM_EVENT or USER_EVENT
   summary text, -- shorter summary of event
   details text -- detailed description of event
);

CREATE INDEX idx_audit_licence_id ON audit_event(licence_id);

CREATE INDEX idx_audit_username ON audit_event(username);

CREATE INDEX idx_audit_event_time ON audit_event(event_time);
