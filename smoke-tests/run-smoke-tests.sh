#!/usr/bin/env bash

export AUTH_URL="https://sign-in-dev.hmpps.service.justice.gov.uk"
export CVL_API_URL="https://create-and-vary-a-licence-api-test2.hmpps.service.justice.gov.uk"
export CVL_URL="https://create-and-vary-a-licence-test2.hmpps.service.justice.gov.uk"

SYSTEM_CLIENT_ID=$(kubectl -n create-and-vary-a-licence-dev get secrets create-and-vary-a-licence -o json  | jq -r '.data.SYSTEM_CLIENT_ID | @base64d')
export SYSTEM_CLIENT_ID

SYSTEM_CLIENT_SECRET=$(kubectl -n create-and-vary-a-licence-dev get secrets create-and-vary-a-licence -o json  | jq -r '.data.SYSTEM_CLIENT_SECRET | @base64d')
export SYSTEM_CLIENT_SECRET

# Remove any old screenshots
rm ./*.png

k6 run --secret-source=file=cvl-smoke-test-secrets cvl-smoke-test.js

