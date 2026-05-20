#!/usr/bin/env bash

# Smoke test for hmpps-hdc-api in the dev environment.
# Requires: jq, kubectl (with access to the licences-dev namespace)

BASE_URL=https://create-and-vary-a-licence-api-dev.hmpps.service.justice.gov.uk
AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
SWAGGER_URL="${BASE_URL}/swagger-ui/index.html?configUrl=/v3/api-docs"
NAMESPACE=create-and-vary-a-licence-dev
# fake PRN for testing
SAR_PRN=A0000AA

PASS=0
FAIL=0

pass() {
  echo "  ✓ $1"
  PASS=$((PASS + 1))
}

fail() {
  echo "  ✗ $1"
  FAIL=$((FAIL + 1))
}

check_status() {
  local description=$1
  local expected=$2
  local actual=$3
  if [[ "${actual}" == "${expected}" ]]; then
    pass "${description} (HTTP ${actual})"
  else
    fail "${description} (expected HTTP ${expected}, got ${actual})"
  fi
}

get_token() {
  local SECRETS
  SECRETS="$(kubectl -n "${NAMESPACE}" get secret create-and-vary-a-licence -o json)"
  local CLIENT_ID
  CLIENT_ID=$(echo "${SECRETS}" | jq -r ".data.SYSTEM_CLIENT_ID | @base64d")
  local CLIENT_SECRET
  CLIENT_SECRET=$(echo "${SECRETS}" | jq -r ".data.SYSTEM_CLIENT_SECRET | @base64d")
  local AUTH_BASIC
  AUTH_BASIC=$(echo -n "${CLIENT_ID}:${CLIENT_SECRET}" | base64 -b 0)

  curl -s -X POST "${AUTH_URL}/oauth/token?grant_type=client_credentials" \
    -H 'Content-Length: 0' \
    -H "Authorization: Basic ${AUTH_BASIC}" | jq -r '.access_token'
}

echo "=== create-and-vary-a-licence-api smoke test (dev) ==="
echo



echo "1. /info"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/info")
check_status "/info is present" "200" "${STATUS}"

echo
echo "2. /health"
HEALTH_RESPONSE=$(curl -s "${BASE_URL}/health")
HEALTH_STATUS=$(echo "${HEALTH_RESPONSE}" | jq -r '.status' 2>/dev/null || echo "UNKNOWN")
if [[ "${HEALTH_STATUS}" == "UP" ]]; then
  pass "/health is healthy (status: UP)"
else
  fail "/health is not healthy (expected status: UP, got: ${HEALTH_STATUS})"
fi

echo
echo "3. Swagger UI"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${SWAGGER_URL}")
check_status "Swagger UI is available" "200" "${STATUS}"

echo
echo "4. SAR endpoint (without credentials)"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/subject-access-request?prn=${SAR_PRN}")
check_status "SAR is secured (unauthenticated request rejected)" "401" "${STATUS}"

echo
echo "5. SAR endpoint (with credentials)"
TOKEN=$(get_token)
STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/subject-access-request?prn=${SAR_PRN}" \
  -H "Authorization: Bearer ${TOKEN}")
if [[ "${STATUS}" == "200" || "${STATUS}" == "204" ]]; then
  pass "SAR request succeeds (HTTP ${STATUS})"
else
  fail "SAR request failed (expected 200 or 204, got ${STATUS})"
fi

echo
echo "========================================="
echo "Results: ${PASS} passed, ${FAIL} failed"
echo "========================================="
if [[ "${FAIL}" -gt 0 ]]; then
  exit 1
fi
