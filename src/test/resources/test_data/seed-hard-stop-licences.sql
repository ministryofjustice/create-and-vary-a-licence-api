insert into licence (
    id,
    kind,
    version,
    responsible_com_id,
    created_by_com_id,
    created_by_ca_id,
    type_code,
    status_code,
    probation_team_code,
    review_date
) values
      (1,'HARD_STOP','1.0',1,null, 9,'AP','APPROVED', 'A01B02', null),
      (2,'CRD','1.0',2,5,null,'AP','APPROVED', 'A01B02', null),
      (3,'HARD_STOP','1.0',1,null,9,'AP','APPROVED', 'A01B02', null);
