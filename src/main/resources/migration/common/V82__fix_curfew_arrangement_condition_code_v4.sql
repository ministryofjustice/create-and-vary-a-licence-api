-- Fixes existing additional_condition rows that were persisted with the wrong condition_code for the
-- "Curfew arrangement" AP additional condition while policy version 4.0 was live.
-- A PR correcting this code (from the old/wrong code to the correct one used in PolicyV4.kt) was missed,
-- so licences created/varied under version 4.0 before the fix was deployed have the wrong code stored.
UPDATE additional_condition
SET condition_code = '0a370862-5426-49c1-b6d4-3d074d78a81a'
WHERE condition_version = '4.0'
  AND condition_code = '52faefcf-15f0-42c5-b908-621b4a7ecdb9';

