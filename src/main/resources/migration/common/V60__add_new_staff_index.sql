CREATE INDEX IF NOT EXISTS idx_staff_kind_id_partial
	ON public.staff (kind, id)
	WHERE kind IS NOT NULL;

CREATE INDEX idx_staff_kind ON staff(kind) WHERE kind IS NOT NULL;
