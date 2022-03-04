-- Licence event will track the major events (and reasons)
CREATE TABLE licence_event (
  id serial NOT NULL constraint licence_event_pk PRIMARY KEY,
  licence_id integer references licence(id) ON DELETE CASCADE,
  event_type varchar2(50),
  username varchar2(100),
  forenames varchar2(100),
  surname varchar2(100),
  event_description text,
  event_time timestamp with time zone default CURRENT_TIMESTAMP
);

CREATE INDEX idx_licence_event_licence_id ON licence_event(licence_id);
CREATE INDEX idx_licence_event_time ON licence_event(event_time);
CREATE INDEX idx_licence_event_type ON licence_event(event_type);
