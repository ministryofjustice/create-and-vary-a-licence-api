BEGIN;

	CREATE TEMP TABLE data_fix_ap_pss_and_pss AS
		SELECT
			l.id,
			l.type_code AS old_type_code
		FROM public.licence l
		WHERE l.type_code IN ('AP_PSS', 'PSS')  AND l.status_code != 'INACTIVE';

	UPDATE public.licence l SET type_code = 'AP' WHERE l.id IN ( SELECT id  FROM data_fix_ap_pss_and_pss );

	INSERT INTO public.audit_event (
		licence_id,
		username,
		full_name,
		event_type,
		summary,
		detail,
		changes
	)
	SELECT
			id,
			'SYSTEM_USER',
			'SYSTEM',
			'SYSTEM_EVENT',
			'Licence type changed to AP',
			format(
					'Licence type changed from %s to AP as part of a data fix',
					old_type_code
			),
			jsonb_build_object(
					'type_code',
					jsonb_build_object(
							'old', old_type_code,
							'new', 'AP'
					)
			)
		FROM data_fix_ap_pss_and_pss;

COMMIT;