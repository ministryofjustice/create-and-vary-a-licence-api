[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fcreate-and-vary-a-licence-api)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#create-and-vary-a-licence-api "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/create-and-vary-a-licence-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/create-and-vary-a-licence-api)
[![codecov](https://codecov.io/gh/ministryofjustice/create-and-vary-a-licence-api/branch/main/graph/badge.svg?token=G7EZ0S2D92)](https://codecov.io/gh/ministryofjustice/create-and-vary-a-licence-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://create-and-vary-a-licence-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

# create-and-vary-a-licence-api

This service provices access to data stored in the licences database via API endpoints.
The main client is the create-and-vary-a-licence (UI) service.
It is built as  docker image and deployed to the MOJ Cloud Platform.

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

There is a script to help, which sets local profiles, port and DB connection properties to the 
values required.

`$ ./run-local.sh`

# Running the unit tests

Unit tests mock all external dependencies and can be run with no dependent containers.

`$ ./gradlew test`

# Running the integration tests

Integration tests use Wiremock to stub any API calls required, and use a local H2 database 
that is seeded with data specific to each test suite.

`$ ./gradlew integrationTest`

# Linting

To locate any linting issues

`$ ./gradlew ktlintcheck`

To apply some fixes following linting

`$ ./gradlew ktlintformat` 

# Static Analysis

We use a tool called Detekt locally to provide code smell
analysis for the project. A baseline currently exists with all the existing
issues at `detekt-baseline.yml`.

To generate/re-generate the baseline:

`$ ./gradlew detektBaseline`

To run Detekt:

`$ ./gradlew detekt`

Detekt provides some helpful reports in HTML as well which can be opened in any
browser. These are available at `build/reports/detekt`.

# Dependencies

List the dependency tree

`$ ./gradlew dependencies`

Pull latest dependencies according to the `build.gradle.kts` file

`$ ./gradlew dependencyUpdates`


# OWASP vulnerability scanning

`$ ./gradlew dependencyCheckAnalyze`


# Kotlin JPA specification

This project makes use of `https://github.com/consoleau/kotlin-jpa-specification-dsl` to wrap the JPA specification
in fluent style Kotlin and make it much more readable when creating the JPA Specifications used in selecting licences
by criteria.

As this dependency is not available in the Maven central repository yet, and JCenter has closed its
services now, we have imported the single-file directly and kept the licence notification in the comments. 
We can wait to see whether the dependency will soon become available in Maven central and import it from there.

# Connecting to licences-db

The licences-db container is the database that is used by the API. Once this is available, a final extra task would be 
to manage the database locally using a SQL database manager application software of your choice (DBeaver, DataGrip etc).


For the purposes of this, we will be using DBeaver. Using Homebrew, install the community version of DBeaver

`brew install --cask dbeaver-community`

Once installed, open up DBeaver and connect to a database using the Database Navigator and the plug type icon to connect to a database.

![database-connection.png](images%2Fdatabase-connection.png)

From the options, select PostgreSQL and click Next.

![postgres-data-source.png](images%2Fpostgres-data-source.png)

Some of the parameters should already be populated but you need to amend the following:

![postgres-configuration.png](images%2Fpostgres-configuration.png)

Once set up, you should see the following tree structure which will allow you to access the various tables in the CVL database locally.

![posttgres-example.png](images%2Fposttgres-example.png)
