# Copilot Instructions for create-and-vary-a-licence-api

Spring Boot (Kotlin) API providing licence data for the Create and Vary a Licence (CVL) service. Deployed as a
Docker image to the MoJ Cloud Platform. Main client is the `create-and-vary-a-licence` UI service.

## Build, test, lint

- Build: `./gradlew clean build`
- Unit tests (mocked dependencies, no containers required): `./gradlew test`
    - Run a single test class:
      `./gradlew test --tests "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceServiceTest"`
    - Run a single test method:
      `./gradlew test --tests "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceServiceTest.shouldDoX"`
- Integration tests (use Wiremock stubs + local H2/Testcontainers DB seeded per suite): `./gradlew integrationTest`
    - Run a single integration test class:
      `./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.AuditIntegrationTest"`
    - All integration test classes must match `*IntegrationTest*`; the `test` task explicitly excludes
      `*.integration.*`.
- Lint: `./gradlew ktlintCheck` (auto-fix with `./gradlew ktlintFormat`)
- Static analysis: `./gradlew detekt` (baseline in `detekt-baseline.xml`/`detekt-baseline-main.xml`; regenerate with
  `./gradlew detektBaseline`)
- `./gradlew check` runs both `ktlintCheck` and `detekt`.
- Run locally with local DB/wiremock containers: `./run-local.sh` (add `--skip-docker` to skip restarting containers).
- Install pre-commit hooks (ktlint + detekt): `./gradlew installLocalGitHook`.

### Subject Access Request (SAR)

If a change affects data models, API responses, or the report template included in SAR output, regenerate the
expected snapshot files with `./gradlew updateSarSnapshots` (review the diff before committing). See the
`update-sar-template` skill (`.github/skills/update-sar-template/SKILL.md`) for the full workflow.

## Architecture

Standard layered Spring Boot structure under
`src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/`:

- `resource/privateApi` — controllers for the internal API consumed by the CVL UI service (role-gated via
  `@PreAuthorize`, e.g. `ROLE_CVL_ADMIN`).
- `resource/publicApi` — controllers (and their own `model` subpackage) for the versioned public-facing API, kept
  separate from private API models so external consumers aren't coupled to internal DTOs.
- `service` — business logic, with feature-based subpackages (`caseload`, `conditions`, `hdc`, `probation`,
  `prisonEvents`, `domainEvents`, `documents`, `jobs`, `policies`, `reports`, `timeserved`, etc).
- `repository` — Spring Data JPA repositories; `repository/model` holds JPA-specific projection/view models distinct
  from API `model`/`entity` classes.
- `entity` — JPA entities (persistence layer), separate from `model`/`model/request`/`model/response` (API DTOs).
- `model/policy` — versioned licence policy definitions (standard/additional conditions for AP and PSS) — these
  encode legally significant licence condition wording, so changes must preserve historical policy versions rather
  than mutate them.
- `migration` — logic for one-off/adhoc data migrations, with `noRetryExceptions` marking failures that should not
  be retried by the migration job runner.
- `config` — cross-cutting Spring configuration (security via `ResourceServerConfiguration` using
  `hmpps-kotlin-spring-boot-starter`, Jackson, WebClient, Notify, App Insights, global `ControllerAdvice`).
- `kotlinjpaspecificationdsl` — a single-file vendored copy of `consoleau/kotlin-jpa-specification-dsl` (not
  published to Maven Central), used to build fluent JPA `Specification`s. Keep the licence notice in comments if
  editing.
- `src/main/resources/migration` — Flyway SQL migrations (`postgresql` for prod schema, `common` shared, `adhoc`
  one-off data fixes).

Integration tests extend `IntegrationTestBase` and stub upstream services (Prison API, Probation/Delius, etc.) with
Wiremock; they run against a per-suite-seeded local database, not real external systems.

## Key conventions

- Public API and private API controllers/models are deliberately kept in separate packages/DTOs — don't reuse
  private API DTOs on public endpoints.
- Endpoints are secured with Spring Security method annotations (`@PreAuthorize("hasAnyRole('...')")`); document the
  required role in the OpenAPI `@Operation`/`@ApiResponses` description, matching existing controllers.
- Licence policy condition text (`model/policy`) is versioned and historical — do not edit existing policy version
  content; add new versions instead.
- Domain events (`service/domainEvents`) and prison/probation event listeners (`service/prisonEvents`) follow the
  HMPPS SQS/domain-events conventions from `hmpps-sqs-spring-boot-starter`.
