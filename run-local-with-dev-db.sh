#
# This script is used to run the Create and Vary a licence API locally with a Postgresql container in dev env.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -dev profile (from application-dev.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around removing the SSL connection to the database and setting the DB properties, SERVER_PORT and client credentials
# to match those used in the docker-compose files.
#

# Server port - avoid clash with prison-api
export SERVER_PORT=8089

# Client id/secret to access local container-hosted services
# Matches with the seeded client details in hmpps-auth for its dev profile
export SYSTEM_CLIENT_ID=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.SYSTEM_CLIENT_ID | @base64d')
export SYSTEM_CLIENT_SECRET=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.SYSTEM_CLIENT_SECRET | @base64d')

# Provide the DB connection details to local container-hosted Postgresql DB
# Match with the credentials set in create-and-vary-a-licence/docker-compose.yml
export DB_SERVER=localhost
export DB_NAME=$(kubectl -n create-and-vary-a-licence-api-dev get secret rds-instance-output -o json | jq -r '.data.database_name | @base64d')
export DB_USER=$(kubectl -n create-and-vary-a-licence-api-dev get secret rds-instance-output -o json | jq -r '.data.database_username | @base64d')
export DB_PASS=$(kubectl -n create-and-vary-a-licence-api-dev get secret rds-instance-output -o json | jq -r '.data.database_password | @base64d')

# Provide URLs to other local container-based dependent services
# Match with ports defined in docker-compose.yml
export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth

# Make the connection without specifying the sslmode=verify-full requirement
export SPRING_DATASOURCE_URL='jdbc:postgresql://${DB_SERVER}/${DB_NAME}'


# Run the application with stdout and dev profiles active
echo "DB port forward 5432:5432"
nohup kubectl -n create-and-vary-a-licence-api-dev port-forward port-forward 5432:5432 &

echo "Starting the API locally"

SPRING_PROFILES_ACTIVE=stdout,dev ./gradlew bootRun

# End
