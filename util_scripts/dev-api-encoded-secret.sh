#!/bin/bash
#
# NAME:  dev-api.sh
# Script to test APIs in the DEV environment
# 
# Parameters:
# 1. CLIENT - the base64-encoded value for <clientId>:<clientSecret>
#
# Example:
# 
# $ ./dev-api.sh <encoded-secret>
#

CLIENT=${1?No encoded client details found}
AUTH_HOST="https://sign-in-dev.hmpps.service.justice.gov.uk"
API_HOST="https://api-dev.prison.service.justice.gov.uk"
COMMUNITY_HOST="https://community-api-secure.test.delius.probation.hmpps.dsd.io"

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

# curl -X GET --location "$API_HOST/api/offenders/A5170DY" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/all" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .


# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X344165/allOffenderManagers" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
curl -X GET --location  "$COMMUNITY_HOST/secure/staff/staffIdentifier/2500012436/managedOffenders?current=true" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .


# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/allOffenderManagers" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/offenders/crn/X356288/contact-summary" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .
# curl -X GET --location  "$COMMUNITY_HOST/secure/staff/staffIdentifier/2500002049/managedOffenders?current=true" -H "Authorization: $AUTH_TOKEN" -H "Accept: application/json" | jq .

# End
