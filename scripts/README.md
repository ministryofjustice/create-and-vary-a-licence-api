### smoke-dev.sh

`smoke-dev.sh` is a bash script that runs a simple smoke test against the create-and-vary-a-licence-api in the dev
environment.

It checks:

1. `/info` endpoint is present (HTTP 200)
2. `/health` endpoint is present and healthy (`status: UP`)
3. Swagger UI is available (HTTP 200)
4. The SAR (`/subject-access-request`) endpoint is secured — an unauthenticated request is rejected with HTTP 401
5. A SAR request with valid credentials succeeds (HTTP 200 or 204)

Dependencies: `jq`, `kubectl` (with access to the `create-and-vary-a-licence-dev` namespace).

Run directly from the `scripts/` directory:

```bash
./smoke-dev.sh
```

### export-additional-conditions.sh

`export-conditions.sh` is a bash script that outputs the conditions for a given 
policy version with v4 being the default policy if the version is not specified.

It provides a CSV containing the following columns:

1. CVL Code - the code of the policy condition
2. CVL Main Heading - the main category heading of the policy condition
3. CVL Detail - the detailed description of the policy condition

Dependencies: `jq`

Run directly from the `scripts/` directory:

```bash
./export-conditions.sh <policy-version>

e.g. ./export-conditions.sh v3
```

### check-events

`check-events/` contains a small Node.js (`.mjs`) tool for exercising licence event flows (e.g. transferring a
prisoner between prisons) against the create-and-vary-a-licence-api dev environment. It authenticates via HMPPS
Auth using client-credentials from a Kubernetes secret, then dispatches to a "mode" handler.

Files:

- `run.sh` — bash entrypoint. Fetches `CLIENT_ID`/`CLIENT_SECRET`/`TEST_PRISON_USER` from the
  `create-and-vary-a-licence-api-smoke-test-client-creds` secret in the `create-and-vary-a-licence-api-dev`
  namespace, then runs `run.mjs`, forwarding all arguments.
- `run.mjs` — parses CLI arguments and dispatches to the handler for the requested mode.
- `transfer.mjs` — implements the `transfer` mode: looks up a prisoner's in-flight licence, checks it belongs to
  the `--from-prison`, and reports the outcome.
- `utils/args.mjs` — dependency-free CLI argument parser (no third-party libraries).
- `utils/client.mjs` — HMPPS Auth token fetching and create-and-vary-a-licence-api HTTP client helpers.
- `utils/prisonApi.mjs` — dependency-free client for the prison-api `transfer-in`, `transfer-out` and `release`
  endpoints, ported from
  [`basm-move-generator/scripts/prison-api`](https://github.com/ministryofjustice/basm-move-generator/blob/main/scripts/prison-api).
  Exports `transferIn`, `transferOut` and `release`, each taking a bearer `token` and prisoner offender number
  (plus mode-specific parameters such as `toLocation`).

Dependencies: `jq`, `kubectl` (with access to the `create-and-vary-a-licence-api-dev` namespace), Node.js.

Run directly from the `scripts/check-events/` directory:

```bash
./run.sh transfer --from-prison=MDI --to-prison=LEI --prison_number=A1234AA
```

Arguments:

- Mode (positional, first argument) — currently only `transfer` is supported.
- `--from-prison=<prisonCode>` — the prison code the prisoner is transferring from.
- `--to-prison=<prisonCode>` — the prison code the prisoner is transferring to.
- `--prison_number=<prisonNumber>` — the prisoner's NOMIS prison number.

### find-merge-discrepancies

`find-merge-discrepancies/` is a one-off bash + Node.js tool that finds licences which are not `INACTIVE` but whose
`noms_id` no longer exists in prisoner-search — a strong signal of a prisoner merge event (NOMIS numbers A and B
merged into one) that was missed/unhandled, leaving a licence pointing at a prison number that's gone.

It runs as a single short-lived Kubernetes pod, so no local DB client, port-forward or local network access to
HMPPS Auth/prisoner-search is required (and in preprod/prod, direct calls to HMPPS Auth's `/oauth/token` from
outside the cluster are rejected, so this step has to happen in-cluster):

1. An `initContainer` (`postgres:16-alpine`) queries the `licence` table directly for all licences where
   `status_code != 'INACTIVE'` and writes them to a shared `emptyDir` volume.
2. The main container (`node:20-alpine`) reads that file, gets an HMPPS Auth token using the CVL API's own system
   client (`SYSTEM_CLIENT_ID`/`SYSTEM_CLIENT_SECRET` on the `create-and-vary-a-licence-api` secret, which has
   `ROLE_PRISONER_SEARCH` in every environment), and looks up the distinct `noms_id`s found in prisoner-search
   (`POST /prisoner-search/prisoner-numbers`).
3. Any licence whose `noms_id` was not returned by prisoner-search is reported as a discrepancy and written to a
   local CSV file, `discrepancies-<env>-<timestamp>.csv`, in the current directory.

Both containers run non-root with a `restricted`-Pod-Security-Standard-compliant security context (required by
Cloud Platform namespaces). The checking logic is split into modular files (`run.mjs` + `utils/*.mjs`, the same
layout as `scripts/check-events`), but since there's no `ConfigMap` or `kubectl cp` step, `run.sh` bundles them
into a single script that's embedded directly as the main container's command. DB/API credentials are wired in
directly via `secretKeyRef` (never fetched/decoded locally). The pod is deleted automatically when the script
exits.

Files:

- `run.sh` — bash entrypoint. Resolves the namespace/auth/prisoner-search URLs for `--env`, bundles `run.mjs` and
  `utils/*.mjs` into a single script (a plain text concatenation that strips `import`/`export` keywords - safe
  here because we control the modules' contents), builds and applies a single pod (embedding the bundled script as
  the main container's command, referencing the `rds-instance-output` / `create-and-vary-a-licence-api` secrets
  via `secretKeyRef`), polls until the pod completes, then splits the main container's logs into progress messages
  (printed to the terminal) and the CSV report (written to a local file).
- `run.mjs` — entrypoint/orchestrator. Reads the queried licences, checks them against prisoner-search, prints
  progress messages to stderr, and prints the CSV report to stdout with each line prefixed `CSV:` (so `run.sh` can
  reliably pick the report lines out of `kubectl logs`, which merges stdout and stderr).
- `utils/args.mjs` — parses the `--input=<path>` CLI arg.
- `utils/csv.mjs` — reads/parses the licences CSV written by the `initContainer`.
- `utils/client.mjs` — gets an HMPPS Auth token and calls prisoner-search; reads `AUTH_URL`, `PRISONER_SEARCH_URL`,
  `CLIENT_ID`, `CLIENT_SECRET` from the environment (set by `run.sh`).

Dependencies: `jq`, `kubectl` (with access to the `create-and-vary-a-licence-api-<env>` namespace and permission to
create pods there).

Run directly from the `scripts/find-merge-discrepancies/` directory:

```bash
./run.sh --env=dev
```

Arguments:

- `--env=<dev|preprod|prod>` — target environment (defaults to `dev`).

Output: a CSV file (`licenceId,nomsId,crn,statusCode,name` per row) is written to the current directory, with just
a header row when no discrepancies are found.
