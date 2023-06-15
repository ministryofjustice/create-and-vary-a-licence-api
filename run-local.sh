#
# This script is used to run the Create and Vary a licence API locally, to interact with
# existing Postresql, localstack, prison-api and hmpps-auth containers.
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
export SYSTEM_CLIENT_ID=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq '.data.SYSTEM_CLIENT_ID | @base64d')
export SYSTEM_CLIENT_SECRET=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq '.data.SYSTEM_CLIENT_SECRET | @base64d')

# Provide the DB connection details to local container-hosted Postgresql DB
# Match with the credentials set in create-and-vary-a-licence/docker-compose.yml
export DB_SERVER=localhost
export DB_NAME=create-and-vary-a-licence-db
export DB_USER=create-and-vary-a-licence
export DB_PASS=create-and-vary-a-licence

# Provide URLs to other local container-based dependent services
# Match with ports defined in docker-compose.yml
export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth

# Make the connection without specifying the sslmode=verify-full requirement
export SPRING_DATASOURCE_URL='jdbc:postgresql://${DB_SERVER}/${DB_NAME}'

# Bring up the licences-db container needed for the application
docker-compose down --remove-orphans && docker-compose pull
docker-compose -f docker-compose.yml up -d
echo "Waiting for container to start..."
until [ "`docker inspect -f {{.State.Health.Status}} licences-db`" == "healthy" ]; do
    sleep 0.1;
done;
echo "Licences database container has been started"

# Run the application with stdout and dev profiles active
SPRING_PROFILES_ACTIVE=stdout,dev ./gradlew bootRun

# End