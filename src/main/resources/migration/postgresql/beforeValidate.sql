/*
Script to run before validating flyway migration scripts.
The following should be idempotent since it will run on every startup...
 */


/*
Amending V10__create_com_details_table.sql:

The original unique constraints for this table were left with the default names.
Postgres generates sensible defaults whereas H2 generates random names.

We have modified the original script to retrospectively name the constraints the same as postgres would have called them.
This has no practical effect in production but enables us to write migrations that can be run in dev to modify the constraints by name.
*/
 UPDATE flyway_schema_history
 SET checksum = -1711289223
 WHERE (version, checksum) = ('10', -2012135289);