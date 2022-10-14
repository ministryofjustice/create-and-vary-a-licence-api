[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fcreate-and-vary-a-licence-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#create-and-vary-a-licence-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/create-and-vary-a-licence-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/create-and-vary-a-licence-api)
[![codecov](https://codecov.io/gh/ministryofjustice/create-and-vary-a-licence-api/branch/main/graph/badge.svg?token=G7EZ0S2D92)](https://codecov.io/gh/ministryofjustice/create-and-vary-a-licence-api)

# create-and-vary-a-licence-api

This service provices access to data stored in the licences database via API endpoints.
The main client is the create-and-vary-a-licence (UI) service.
It is built as  docker image and deployed to the MOJ Cloud Platform.

# Dependencies

This service requires a postgresql database.

# Building the project

Tools required:

* JDK v18+
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


# Kotlin JPA specification

This project makes use of `https://github.com/consoleau/kotlin-jpa-specification-dsl` to wrap the JPA specification
in fluent style Kotlin and make it much more readable when creating the JPA Specifications used in selecting licences
by criteria.

As this dependency is not available in the Maven central repository yet, and JCenter has closed its
services now, we have imported the single-file directly and kept the licence notification in the comments. 
We can wait to see whether the dependency will soon become available in Maven central and import it from there.
