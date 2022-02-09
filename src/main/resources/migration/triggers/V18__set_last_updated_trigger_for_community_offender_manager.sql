--------------------------------------------------------------------------------
----- Create triggers for community_offender_manager table -----
--------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION community_offender_manager_updated() RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_timestamp = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER community_offender_manager_updated BEFORE UPDATE
    ON community_offender_manager FOR EACH ROW EXECUTE PROCEDURE
    community_offender_manager_updated();

--------------------------------------------------------------------------------
----- Create triggers for community_offender_manager_licence mapping table -----
--------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION community_offender_manager_licence_mailing_list_updated() RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated_timestamp = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ LANGUAGE 'plpgsql';

CREATE TRIGGER community_offender_manager_licence_mailing_list_updated BEFORE UPDATE
    ON community_offender_manager_licence_mailing_list FOR EACH ROW EXECUTE PROCEDURE
    community_offender_manager_licence_mailing_list_updated();
