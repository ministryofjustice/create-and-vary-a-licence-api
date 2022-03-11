#!/bin/bash
#
# Script to test APIs in the DEV environment
# 
# Parameters:
# 1. CLIENT - the base64-encoded value for <clientId>:<clientSecret>
#
# Example:
# 
# $ ./dev-api-encoded-secret.sh <encoded-secret>
#

CLIENT=${1?No encoded client details found}
AUTH_HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"
API_HOST="https://api-dev.prison.service.justice.gov.uk"
COMMUNITY_HOST="https://community-api-secure.test.delius.probation.hmpps.dsd.io"
PRISON_REGISTER_HOST="https://prison-register-dev.hmpps.service.justice.gov.uk"

# For running against DEV
CVL_API="https://create-and-vary-a-licence-api-dev.hmpps.service.justice.gov.uk"

# For running against localhost:8089
# CVL_API="http://localhost:8089"

TOKEN_RESPONSE=$(curl -s -k -d "" -X POST "$AUTH_HOST/auth/oauth/token?grant_type=client_credentials" -H "Authorization: Basic $CLIENT")
TOKEN=$(echo "$TOKEN_RESPONSE" | jq -er .access_token)
if [[ $? -ne 0 ]]; then
  echo "Failed to read token from credentials response"
  echo "$TOKEN_RESPONSE"
  exit 1
fi

AUTH_TOKEN="Bearer $TOKEN"

echo "---------------------------------------------------------------"
echo $AUTH_TOKEN
echo "---------------------------------------------------------------"

# Prison API

# curl -X GET --location "$API_HOST/api/offenders/A5170DY" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location "$API_HOST/api/offenders/G9786GC" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location "$API_HOST/api/offenders/G1086GO" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .

# Community API

# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/all" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X344165/allOffenderManagers" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/staffIdentifier/2500012436/managedOffenders?current=true" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/staffIdentifier/2500012436" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/username/lucybennett" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/username/timharrison" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/allOffenderManagers" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/contact-summary" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/staffIdentifier/2500002049/managedOffenders?current=true" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .

# Prison register

# curl -v -X GET "$PRISON_REGISTER_HOST/prisons" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -v -X GET "$PRISON_REGISTER_HOST/secure/prisons/id/CFI/offender-management-unit/email-address" -H "Authorization: $AUTH_TOKEN" -H "Accept: text/plain"
# curl -v -X PUT "$PRISON_REGISTER_HOST/secure/prisons/id/SWI/offender-management-unit/email-address" -d "test@test.com" -H "Authorization: $AUTH_TOKEN" -H "Content-Type: text/plain" -H "Accept: text/plain"
# curl -v -X GET "$PRISON_REGISTER_HOST/secure/prisons/id/SWI/offender-management-unit/email-address" -H "Authorization: $AUTH_TOKEN" -H "Accept: text/plain"
# curl -v -X GET "$PRISON_REGISTER_HOST/secure/prisons/id/UKI/offender-management-unit/email-address" -H "Authorization: $AUTH_TOKEN" -H "Accept: text/plain"
# curl -v -X GET "$PRISON_REGISTER_HOST/secure/prisons/id/UPI/offender-management-unit/email-address" -H "Authorization: $AUTH_TOKEN" -H "Accept: text/plain"
# curl -v -X GET "$PRISON_REGISTER_HOST/secure/prisons/id/PRI/offender-management-unit/email-address" -H "Authorization: $AUTH_TOKEN" -H "Accept: text/plain"

# Create and vary a licence API

# curl -v -X GET "$CVL_API/licence/match?prison=MDI&status=APPROVED&nomsId=A7814DY" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET "$CVL_API/config/pdu-contacts" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .

# End
