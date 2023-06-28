DO
$$
    declare
        l         record;
        licenceId bigint;
    begin
        for l in select id from licence order by id desc
            loop

                licenceId = l.id;
                delete from additional_condition_upload_detail where licence_id = licenceId;
                delete
                from additional_condition_upload_summary
                where additional_condition_id in (select id
                                                  from additional_condition
                                                  where licence_id = licenceId);
                delete
                from additional_condition_data
                where additional_condition_id in (select id
                                                  from additional_condition
                                                  where licence_id = licenceId);
                delete from additional_condition where licence_id = licenceId;
                delete from standard_condition where licence_id = licenceId;
                delete from bespoke_condition where licence_id = licenceId;
                delete from audit_event where licence_id = licenceId;
                delete from licence_event where licence_id = licenceId;
                delete from licence where id = licenceId;
            end loop;
    end;
$$;