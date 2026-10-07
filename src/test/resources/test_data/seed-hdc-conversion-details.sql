insert into additional_condition (
  licence_id,
  condition_version,
  condition_category,
  condition_code,
  condition_sequence,
  condition_text,
  expanded_condition_text,
  condition_type
)
values (
  1,
  '1.0',
  'Freedom of movement',
  '9ae2a336-3491-4667-aaed-dd852b09b4b9',
  1,
  'Not to enter exclusion zone [EXCLUSION ZONE DESCRIPTION]',
  'Not to enter exclusion zone Town centre',
  'AP'
);

insert into additional_condition_data (
  additional_condition_id,
  data_sequence,
  data_field,
  data_value
)
values (
  (select max(id) from additional_condition),
  1,
  'outOfBoundArea',
  'Town centre'
);

insert into bespoke_condition (licence_id, condition_sequence, condition_text)
values (1, 1, 'Do not contact Person A');
