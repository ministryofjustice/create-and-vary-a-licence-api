CREATE INDEX IF NOT EXISTS idx_staff_kind_id_partial
	ON public.staff (kind, id);

CREATE INDEX IF NOT EXISTS idx_staff_kind
	ON staff(kind);
