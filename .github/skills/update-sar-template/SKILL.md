---
name: update-sar-template
description: How to update the Subject Access Request (SAR) mustache template, the SAR response model/builder, and how to regenerate the expected snapshot files used by SAR tests in this repo. Use when asked to add/change a field in the SAR report, edit sar_create-and-vary-a-licence-api.mustache, or regenerate SAR snapshots/expected files.
metadata:
  author: create-and-vary-a-licence-api
  version: "1.0"
---

# Updating Subject Access Request (SAR) templates

This service exposes a Subject Access Request (SAR) data API and an HTML report, built using the
`hmpps-subject-access-request-lib`. Changes to what data is returned or rendered require touching three
layers, in this order, and then regenerating snapshot files.

## 1. Data model — what fields exist

DTOs returned by the SAR API live in:
`src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/resource/publicApi/model/subjectAccessRequest/`

Add/modify fields here first (e.g. `SarLicence`, `SarHdcInfo`). Use `@field:Schema(description = ...)` for
OpenAPI docs, matching existing fields.

## 2. Response builder — how fields are populated

`src/main/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/service/publicApi/SubjectAccessRequestResponseBuilder.kt`
maps internal domain/entity data onto the SAR DTOs above. This is where you decide *when* a field is populated
(e.g. `hdcInfo` is only set `if (licence is ModelHdcCase)`, which covers both `HDC` and `HDC_VARIATION` licence
kinds). Prefer null-guarding data here over conditional logic in the template — the template should just render
what's present.

## 3. Mustache template — how fields are rendered

`src/main/resources/templates/sar_create-and-vary-a-licence-api.mustache` renders the JSON produced by the
builder into the HTML report shown to the Offender SAR team.

Conventions used throughout this template:
- Wrap every scalar value in `{{ optionalValue fieldName }}` (renders "No data held"/blank gracefully for nulls).
- Wrap date/time fields in `{{ optionalValue (formatDate fieldName) }}`.
- For nested objects, use `{{#field}} ... {{/field}}` for the populated case and `{{^field}}<p>No data held</p>{{/field}}` for the absent case — this is how conditional sections are done (there is no `{{#eq}}`/string-comparison helper in use). To gate a whole section on "only for HDC licences", rely on the builder leaving the field `null` for other kinds rather than comparing `kind` in the template.
- For lists, follow the same `{{#list}}...{{/list}}` / `{{^list}}<tr><td>No data held</td></tr>{{/list}}` pattern used for `standardLicenceConditions`, `bespokeConditions`, etc.

Available mustache helpers (from `TemplateHelpers` in `hmpps-subject-access-request-lib`): `optionalValue`,
`optionalString`, `formatDate`, `getOrDefault`, `getIndexPlusOne`, `getPrisonName`, `getUserLastName`,
`getLocationNameByDpsId`, `getLocationNameByNomisId`, `convertBoolean`, `buildDate`, `buildDateNumber`, `eq`,
`convertCamelCase`, `inlineAttachment`/`inlineAttachmentContent`.

## 4. Tests and regenerating snapshot/expected files

The SAR tests live in
`src/test/kotlin/uk/gov/justice/digital/hmpps/createandvaryalicenceapi/integration/publicApi/SubjectAccessRequestIntegrationTest.kt`,
which mixes in test interfaces from `hmpps-subject-access-request-test-support`:

| Interface | Verifies | Expected file (under `src/test/resources/sar/`) |
|---|---|---|
| `SarApiDataTest` | JSON returned by the SAR data API | `sar-api-response.json` |
| `SarReportTest` | Rendered HTML report from the mustache template | `sar-generated-report.html` |
| `SarJpaEntitiesTest` | JPA entity schema shape (catches unreviewed entity changes) | `entity-schema.json` |
| `SarFlywaySchemaTest` | Flyway schema version pinned in `application-test.yml` (`hmpps.sar.tests.expected-flyway-schema-version`) | n/a (just a version number) |

Test data is seeded via `@Sql("classpath:test_data/seed-sar-content-licence-id.sql")` on each test method.

**After changing the model/builder/template, regenerate the expected files instead of hand-editing JSON/HTML.**

The simplest way is the `updateSarSnapshots` Gradle task, which runs the SAR integration test with
`SAR_GENERATE_ACTUAL=true` and copies the resulting `.log` files into `src/test/resources/sar/` (stripping the
`.log` suffix and removing the intermediate files):

```shell
./gradlew updateSarSnapshots
```

Always review the resulting diff in `src/test/resources/sar/` before committing — an unreviewed diff can
silently leak or drop personal data from the SAR report.

If you need to do this manually (e.g. to inspect the raw `.log` files before deciding whether to keep them), run:

```shell
SAR_GENERATE_ACTUAL=true ./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi.SubjectAccessRequestIntegrationTest"
```

This writes the *actual* output next to the expected files with a `.log` suffix under `src/test/resources/` (not
`src/test/resources/sar/`), e.g. `src/test/resources/sar-api-response.json.log` and
`src/test/resources/sar-generated-report.html.log` (and `entity-schema.json.log` if entities changed). Move each
file into `src/test/resources/sar/`, stripping the `.log` suffix:

```shell
mv src/test/resources/sar-api-response.json.log src/test/resources/sar/sar-api-response.json
mv src/test/resources/sar-generated-report.html.log src/test/resources/sar/sar-generated-report.html
mv src/test/resources/entity-schema.json.log src/test/resources/sar/entity-schema.json
```

Then re-run the integration test **without** `SAR_GENERATE_ACTUAL` to confirm it now passes:

```shell
./gradlew integrationTest --tests "uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi.SubjectAccessRequestIntegrationTest"
```

To preview the generated PDF version of the report (produced automatically by `SarReportTest`):

```shell
/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome \
  --headless --disable-gpu --print-to-pdf=example-cvl-api.pdf src/test/resources/sar/sar-generated-report.html
```

**Manual review is important**: always inspect the generated HTML/JSON diff before committing — an unreviewed
diff can silently leak or drop personal data from the SAR report.

## 5. Entity schema / Flyway version

If your change adds/modifies a JPA entity (not just the SAR DTO/template), `SarJpaEntitiesTest` will also need
regenerating (same `SAR_GENERATE_ACTUAL=true` run covers it). If a Flyway migration changes the schema version,
update `hmpps.sar.tests.expected-flyway-schema-version` in `src/test/resources/application-test.yml` — treat this
as a prompt to check with the team whether the SAR data/report also needs updating for the new column/table.
