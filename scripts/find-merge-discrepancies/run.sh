#!/usr/bin/env bash
set -euo pipefail

# Finds licences that are not INACTIVE but whose noms_id no longer exists in
# prisoner-search (a strong signal of a missed/unhandled prisoner merge event).
#
# Requires: kubectl (with access to the create-and-vary-a-licence-api-<env> namespace and
# permission to create pods there), jq.
#
# Runs as a single Kubernetes pod:
#   - an initContainer (postgres:16-alpine) queries the RDS instance directly for candidate
#     licences and writes them to a shared emptyDir volume
#   - the main container (node:20-alpine) runs check.mjs (embedded directly) to check those licences against prisoner-search
#
# This has to run in-cluster (rather than calling HMPPS Auth/prisoner-search from a local
# machine) because preprod/prod reject direct external calls to the HMPPS Auth /oauth/token
# endpoint; only in-cluster traffic is accepted.
#
# The checking logic itself lives in run.mjs + utils/*.mjs (same layout as scripts/check-events),
# for readability. Since there's no ConfigMap or `kubectl cp` step, run.sh bundles those files
# into a single script (bundle.mjs, further down) that's embedded directly as the pod's command.
#
# Usage:
#   ./run.sh --env=dev

cd "$(dirname "$0")"

ENV="dev"
for arg in "$@"; do
  case "$arg" in
    --env=*) ENV="${arg#--env=}" ;;
  esac
done

case "$ENV" in
  dev)
    NAMESPACE="create-and-vary-a-licence-api-dev"
    AUTH_URL="https://sign-in-dev.hmpps.service.justice.gov.uk/auth"
    PRISONER_SEARCH_URL="https://prisoner-search-dev.prison.service.justice.gov.uk"
    ;;
  preprod)
    NAMESPACE="create-and-vary-a-licence-api-preprod"
    AUTH_URL="https://sign-in-preprod.hmpps.service.justice.gov.uk/auth"
    PRISONER_SEARCH_URL="https://prisoner-search-preprod.prison.service.justice.gov.uk"
    ;;
  prod)
    NAMESPACE="create-and-vary-a-licence-api-prod"
    AUTH_URL="https://sign-in.hmpps.service.justice.gov.uk/auth"
    PRISONER_SEARCH_URL="https://prisoner-search.prison.service.justice.gov.uk"
    ;;
  *)
    echo "Unknown --env=${ENV}. Expected dev, preprod or prod." >&2
    exit 1
    ;;
esac

echo "Using namespace: ${NAMESPACE}" >&2

POD_NAME="find-merge-discrepancies-$(date +%d%m%Y-%H%M%S)"

cleanup() {
  kubectl -n "${NAMESPACE}" delete pod "${POD_NAME}" --ignore-not-found --wait=false >/dev/null 2>&1 || true
}
trap cleanup EXIT

# Bundles run.mjs + utils/*.mjs into a single script, so they can be embedded directly as the pod
# command (no ConfigMap or `kubectl cp` needed). This is a plain text concatenation, not a real
# bundler: it works because we control the modules' contents, each export is a single
# `const`/`function` declaration on its own line, and every import is a same-directory relative
# import of one of the other bundled files.
bundle() {
  for file in utils/args.mjs utils/csv.mjs utils/client.mjs run.mjs; do
    sed -E "/^import .* from ['\"]\.\//d" "$file" | sed -E 's/^export (const|async function|function) /\1 /'
    echo
  done
}
BUNDLED_SCRIPT="$(bundle)"

# Cloud Platform namespaces enforce the Kubernetes "restricted" Pod Security Standard, which
# rejects pods/containers that run as root. The postgres image's default user is root, so the
# init container must explicitly run as the image's non-root "postgres" user (uid/gid 999); the
# node container runs as an arbitrary non-root uid/gid.
#
# DB and API credentials are wired in directly via secretKeyRef (never read/decoded locally):
#   - rds-instance-output: real RDS creds for this namespace
#   - create-and-vary-a-licence-api: the API's own SYSTEM_CLIENT_ID/SECRET, which has
#     ROLE_PRISONER_SEARCH in every environment (dev/preprod/prod), so no dev-only test
#     credentials are required.
POD_JSON="$(
  jq -n \
    --arg name "${POD_NAME}" \
    --arg namespace "${NAMESPACE}" \
    --arg authUrl "${AUTH_URL}" \
    --arg prisonerSearchUrl "${PRISONER_SEARCH_URL}" \
    --arg envName "${ENV}" \
    --arg checkScript "${BUNDLED_SCRIPT}" \
    '{
      apiVersion: "v1",
      kind: "Pod",
      metadata: {name: $name, namespace: $namespace, labels: {app: "find-merge-discrepancies"}},
      spec: {
        restartPolicy: "Never",
        securityContext: {runAsNonRoot: true, seccompProfile: {type: "RuntimeDefault"}},
        volumes: [{name: "data", emptyDir: {}}],
        initContainers: [{
          name: "query",
          image: "postgres:16-alpine",
          securityContext: {
            runAsUser: 999, runAsGroup: 999, allowPrivilegeEscalation: false, capabilities: {drop: ["ALL"]}
          },
          env: [
            {name: "HOME", value: "/tmp"},
            {name: "PGHOST", valueFrom: {secretKeyRef: {name: "rds-instance-output", key: "rds_instance_address"}}},
            {name: "PGDATABASE", valueFrom: {secretKeyRef: {name: "rds-instance-output", key: "database_name"}}},
            {name: "PGUSER", valueFrom: {secretKeyRef: {name: "rds-instance-output", key: "database_username"}}},
            {name: "PGPASSWORD", valueFrom: {secretKeyRef: {name: "rds-instance-output", key: "database_password"}}}
          ],
          resources: {requests: {memory: "128Mi", cpu: "50m"}, limits: {memory: "256Mi"}},
          command: ["sh", "-c"],
          args: [
            "psql -At -F, -c \"SELECT id, noms_id, crn, status_code FROM licence WHERE status_code != '\''INACTIVE'\'' AND noms_id IS NOT NULL ORDER BY id\" > /data/licences.csv"
          ],
          volumeMounts: [{name: "data", mountPath: "/data"}]
        }],
        containers: [{
          name: "check",
          image: "node:20-alpine",
          securityContext: {
            runAsUser: 1000, runAsGroup: 1000, allowPrivilegeEscalation: false, capabilities: {drop: ["ALL"]}
          },
          env: [
            {name: "AUTH_URL", value: $authUrl},
            {name: "PRISONER_SEARCH_URL", value: $prisonerSearchUrl},
            {name: "ENV_NAME", value: $envName},
            {name: "CLIENT_ID", valueFrom: {secretKeyRef: {name: "create-and-vary-a-licence-api", key: "SYSTEM_CLIENT_ID"}}},
            {name: "CLIENT_SECRET", valueFrom: {secretKeyRef: {name: "create-and-vary-a-licence-api", key: "SYSTEM_CLIENT_SECRET"}}}
          ],
          resources: {requests: {memory: "256Mi", cpu: "100m"}, limits: {memory: "512Mi"}},
          command: ["node", "--input-type=module", "-e", $checkScript, "--", "--input=/data/licences.csv"],
          volumeMounts: [{name: "data", mountPath: "/data"}]
        }]
      }
    }'
)"

echo "Starting pod (this can take a minute to pull images/schedule)..." >&2
echo "${POD_JSON}" | kubectl -n "${NAMESPACE}" apply -f -

echo "Waiting for the pod to finish (DB query, then prisoner-search checks)..." >&2
PHASE=""
for _ in $(seq 1 90); do
  PHASE="$(kubectl -n "${NAMESPACE}" get pod "${POD_NAME}" -o jsonpath='{.status.phase}' 2>/dev/null || true)"
  [ "${PHASE}" = "Succeeded" ] || [ "${PHASE}" = "Failed" ] && break
  sleep 2
done

if [ "${PHASE}" != "Succeeded" ] && [ "${PHASE}" != "Failed" ]; then
  echo "Pod did not finish within the timeout. Check it with:" >&2
  echo "  kubectl -n ${NAMESPACE} describe pod ${POD_NAME}" >&2
  exit 1
fi

if [ "${PHASE}" = "Failed" ]; then
  echo "Pod failed. Query container logs:" >&2
  kubectl -n "${NAMESPACE}" logs "${POD_NAME}" -c query >&2 || true
  echo "Check container logs:" >&2
  kubectl -n "${NAMESPACE}" logs "${POD_NAME}" -c check >&2 || true
  exit 1
fi

# check.mjs prefixes its CSV report lines with "CSV:" and sends everything else (progress
# messages) to stderr, so they can be told apart even though `kubectl logs` merges both streams
# into one combined log. Progress messages are echoed to the terminal; the CSV lines are written
# to a local output file.
OUTPUT_FILE="discrepancies-${ENV}-$(date +%Y%m%dT%H%M%S).csv"
CHECK_LOGS="$(kubectl -n "${NAMESPACE}" logs "${POD_NAME}" -c check)"
echo "${CHECK_LOGS}" | grep -v '^CSV:' >&2 || true
echo "${CHECK_LOGS}" | sed -n 's/^CSV://p' > "${OUTPUT_FILE}"

ROW_COUNT="$(($(wc -l < "${OUTPUT_FILE}" | tr -d ' ') - 1))"
echo "Wrote ${ROW_COUNT} discrepancy row(s) to ${OUTPUT_FILE}" >&2
