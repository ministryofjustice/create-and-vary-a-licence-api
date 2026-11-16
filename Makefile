#
# Run the Create and Vary a licence API locally with a Postgresql container.
#
# Runs with the default spring profile (application.yaml) supplemented by the dev profile (application-dev.yml).
# The environment variables below override spring profile properties - they remove the SSL connection to the
# database and set the DB properties, SERVER_PORT and client credentials to match docker-compose.yml.
#
# Targets:
#   make run               start any missing containers, then run the service
#   make run-skip-docker   run the service without touching containers
#   make stop              stop the containers (service is not started)
#   make clean             gradle clean, and remove containers and volumes (keeps images; service is not started)
#   make update            upgrade the gradle wrapper to the latest release, then run useLatestVersions
#   make prune             remove containers, volumes and images (service is not started)
#
# Options:
#   DEBUG=1                enable jvm debugging - requires remote debug being set up in intellij
#

SHELL := /bin/bash

NS := create-and-vary-a-licence-api-dev
secret = $(shell kubectl -n $(NS) get secrets create-and-vary-a-licence-api -o json | jq -r '.data.$(1) | @base64d')

DEBUG_FLAG := $(if $(DEBUG),--debug-jvm,)

.PHONY: run run-skip-docker docker-up stop clean update prune

run: docker-up run-skip-docker

# Only creates/starts containers that are missing or stopped; running ones are left untouched
docker-up:
	docker compose -f docker-compose.yml up -d --wait --remove-orphans

stop:
	docker compose stop

clean:
	./gradlew clean
	docker compose down --remove-orphans --volumes

update:
	./gradlew wrapper --gradle-version latest
	./gradlew useLatestVersions

prune:
	docker compose down --remove-orphans --volumes --rmi all

# Server port - avoid clash with prison-api
run-skip-docker: export SERVER_PORT=8089

# Secrets from the dev namespace
run-skip-docker: export OS_PLACES_API_KEY=$(call secret,OS_PLACES_API_KEY)
run-skip-docker: export SYSTEM_CLIENT_ID=$(call secret,SYSTEM_CLIENT_ID)
run-skip-docker: export SYSTEM_CLIENT_SECRET=$(call secret,SYSTEM_CLIENT_SECRET)
run-skip-docker: export NOTIFY_API_KEY=$(call secret,NOTIFY_API_KEY)

# Local container-hosted Postgresql DB - matches docker-compose.yml
run-skip-docker: export DB_SERVER=localhost
run-skip-docker: export DB_NAME=create-and-vary-a-licence-db
run-skip-docker: export DB_USER=cvl
run-skip-docker: export DB_PASS=cvl
# Connect without the sslmode=verify-full requirement; placeholders are resolved by spring
run-skip-docker: export SPRING_DATASOURCE_URL=jdbc:postgresql://$${DB_SERVER}/$${DB_NAME}

run-skip-docker: export SPRINGDOC_API_DOCS_ENABLED=true
run-skip-docker: export SPRINGDOC_SWAGGER_UI_ENABLED=true
run-skip-docker: export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth

# Feature toggles
run-skip-docker: export HDC_ENABLED=true
run-skip-docker: export USE_CURRENT_HDC_STATUS=false
run-skip-docker: export REMAND_ENABLED=true

run-skip-docker:
	@echo "Starting the API locally"
	SPRING_PROFILES_ACTIVE=stdout,dev ./gradlew bootRun $(DEBUG_FLAG)
