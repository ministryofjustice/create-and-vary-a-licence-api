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
