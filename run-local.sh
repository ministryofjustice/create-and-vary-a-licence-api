#
# This script is used to run the Create and Vary a licence API locally with a Postgresql container.
#
# It runs with a combination of properties from the default spring profile (in application.yaml) and supplemented
# with the -dev profile (from application-dev.yml). The latter overrides some of the defaults.
#
# The environment variables here will also override values supplied in spring profile properties, specifically
# around removing the SSL connection to the database and setting the DB properties, SERVER_PORT and client credentials
# to match those used in the docker-compose files.
#
# Args:
# * --debug = enables jvm debugging - requires remote debug being set up in intellij to start the app
# * --skip-docker = prevent tear down and refresh of docker containers
#

restart_docker () {
  # Stop the back end containers
  echo "Bringing down current containers ..."
  docker compose down --remove-orphans

  #Prune existing containers
  #Comment in if you wish to perform a fresh install of all containers where all containers are removed and deleted
  #You will be prompted to continue with the deletion in the terminal
  docker system prune --all

  echo "Pulling back end containers ..."
  docker compose pull
  docker compose -f docker-compose.yml up -d

  echo "Waiting for back end containers to be ready ..."
  until [ "`docker inspect -f {{.State.Health.Status}} licences-db`" == "healthy" ]; do
      sleep 0.1;
  done;
  until [ "`docker inspect -f {{.State.Health.Status}} localstack-api`" == "healthy" ]; do
      sleep 0.1;
  done;

  echo "Back end containers are now ready"
}

# Server port - avoid clash with prison-api
export SERVER_PORT=8089

# Client id/secret to access local container-hosted services
# Matches with the seeded client details in hmpps-auth for its dev profile
export OS_PLACES_API_KEY=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.OS_PLACES_API_KEY | @base64d')
export SYSTEM_CLIENT_ID=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.SYSTEM_CLIENT_ID | @base64d')
export SYSTEM_CLIENT_SECRET=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.SYSTEM_CLIENT_SECRET | @base64d')

# Provide the DB connection details to local container-hosted Postgresql DB
# Match with the credentials set in create-and-vary-a-licence/docker-compose.yml
export DB_SERVER=localhost
export DB_NAME=create-and-vary-a-licence-db
export DB_USER=cvl
export DB_PASS=cvl

# Provide Notify details to access Notify
# Match with the API key in hmpps-auth set for its dev profile
export NOTIFY_API_KEY=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json  | jq -r '.data.NOTIFY_API_KEY | @base64d')

# Provide URLs to other local container-based dependent services
# Match with ports defined in docker-compose.yml
export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth

# Make the connection without specifying the sslmode=verify-full requirement
export SPRING_DATASOURCE_URL='jdbc:postgresql://${DB_SERVER}/${DB_NAME}'

# Feature toggles
export FEATURE_TOGGLE_TIMESERVED_ENABLED=true
export TIME_SERVED_PRISONS="MDI, BAI, BNI"
export HDC_ENABLED=true
export LAO_ENABLED=true
export USE_CURRENT_HDC_STATUS=false

SKIP_DOCKER=false
DEBUG=""

# Parse all arguments
for arg in "$@"; do
  case $arg in
    --skip-docker)
      SKIP_DOCKER=true
      ;;
    --debug)
      DEBUG="--debug-jvm"
      ;;
    *)
      echo "Unknown argument: $arg"
      ;;
  esac
done

# Conditionally restart Docker
if [[ $SKIP_DOCKER == false ]]; then
  restart_docker
fi


# Run the application with stdout and dev profiles active
echo "Starting the API locally"

SPRING_PROFILES_ACTIVE=stdout,dev ./gradlew bootRun $DEBUG

# End
