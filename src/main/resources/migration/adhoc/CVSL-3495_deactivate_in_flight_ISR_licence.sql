/*

	ACCEPTANCE CRITERIA
		AC1 -  Deactivate licences IN PROGRESS, SUBMITTED, APPROVED
		GIVEN a licence in case list is IN PROGRESS
		OR SUBMITTED OR APPROVED
		AND the Release Date is after the Progression model live date (TBC)
		WHEN The new Version 4 policy is applicable
		THEN Deactivate the licence

	The progression_model_live_date (TBC) needs to be set before this is run format on data param YYYY-MM-DD
*/

BEGIN;

	UPDATE public.licence l
		SET status_code = 'INACTIVE',
			date_last_updated = CURRENT_TIMESTAMP
			WHERE l.status_code IN ('IN_PROGRESS', 'SUBMITTED', 'APPROVED')
			  AND l.licence_start_date > :progression_model_live_date;

COMMIT;
