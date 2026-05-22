insert into staff(kind, staff_identifier, username, email, first_name, last_name, last_updated_timestamp)
values
	( 'PRISON_USER', null, 'SubmittedbyUserName', 'testClient@prison.gov.uk', 'Prison', 'User',NOW()),
	( 'COMMUNITY_OFFENDER_MANAGER', 4, 'SubmittedByUserName', 'testClient@prison.gov.uk', 'Prison', 'User', NOW()),
	( 'COMMUNITY_OFFENDER_MANAGER', 1, 'submittedByUserName', 'testClient@prison.gov.uk', 'Prison', 'User', NOW()),
	( 'PRISON_USER', null, 'CreatedByUserName', 'testClient@prison.gov.uk', 'Prison', 'User',NOW()),
	( 'COMMUNITY_OFFENDER_MANAGER', 2, 'createdByUserName', 'testClient@prison.gov.uk', 'Prison', 'User',NOW()),
	( 'PRISON_USER', null, 'ApprovedByUsername', 'testClient@prison.gov.uk', 'Approvedbyfirstname', 'Approvedbylastname',NOW()),
	( 'COMMUNITY_OFFENDER_MANAGER', 3, 'approvedByUsername', 'testClient@prison.gov.uk', 'Prison', 'User',NOW());

