# create-and-vary-a-licence-api

This service provices access to data stored in the licences database via API endpoints.
The main client is the create-and-vary-a-licence (UI) service.
It is built as  docker image and deployed to the MOJ Cloud Platform.

# Dependencies

This service requires a postgresql database.

# Building the project

Tools required:

* JDK v16+
* Kotlin
* docker
* docker-compose

## Install gradle

`$ ./gradlew`

`$ ./gradlew clean build`

# Running the service

Start up the docker dependencies using the docker-compose file in the `create-and-vary-a-licence` service

There is a script to help, which sets local profiles, port and DB connection properties to the 
values required.

`$ ./run-local.sh`

Or, to run with default properties set in the docker-compose file

`$ docker-compose pull && docker-compose up`

Or, to use default port and properties

`$ SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun`


# Running the unit tests

Unit tests mock all external dependencies and can be run with no dependent containers.

`$ ./gradlew test`

# Running the integration tests

Integration tests use Wiremock to stub any API calls required, and use a local H2 database 
that is seeded with data specific to each test suite.

`$ ./gradlew integrationTest`

# Linting

`$ ./gradlew ktlintcheck`

# OWASP vulnerability scanning

`$ ./gradlew dependencyCheckAnalyze`

