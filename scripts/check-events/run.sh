#!/usr/bin/env bash

cd $(dirname "$0")

CREDS=$(kubectl -n create-and-vary-a-licence-api-dev get secret create-and-vary-a-licence-api-smoke-test-client-creds -o json)

CLIENT_ID=$(echo $CREDS | jq -r '.data.CLIENT_CREDS_CLIENT_ID | @base64d') \
CLIENT_SECRET=$(echo $CREDS | jq -r '.data.CLIENT_CREDS_CLIENT_SECRET | @base64d') \
TEST_PRISON_USER=$(echo $CREDS | jq -r '.data.TEST_PRISON_USER | @base64d') \
node ./run.mjs "$@"
