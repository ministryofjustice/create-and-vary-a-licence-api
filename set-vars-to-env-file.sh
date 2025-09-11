#!/bin/bash
#
# This script creates a .env file to allow setting env vars to run the Spring app locally in an IDE.
# Run with: ./set-vars-to-local-env.sh
#
# Add the following to IntelliJ Run/Debug config's "Environment variables":
#    /Users/<<YOUR-USER-DIR>>/env-config/cvl.env
#
# (Requires Docker + kubectl + jq)
#

set -e

# --- Strings (static values) ---
export SERVER_PORT=8089
export DB_SERVER=localhost
export DB_NAME=create-and-vary-a-licence-db
export DB_USER=cvl
export DB_PASS=cvl
export HMPPS_AUTH_URL=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_SERVER}/${DB_NAME}"

# --- Secrets (from K8s) ---
echo "Getting OS_PLACES_API_KEY..."
export OS_PLACES_API_KEY=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json | jq -r '.data.OS_PLACES_API_KEY | @base64d')

echo "Getting SYSTEM_CLIENT_ID..."
export SYSTEM_CLIENT_ID=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json | jq -r '.data.SYSTEM_CLIENT_ID | @base64d')

echo "Getting SYSTEM_CLIENT_SECRET..."
export SYSTEM_CLIENT_SECRET=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json | jq -r '.data.SYSTEM_CLIENT_SECRET | @base64d')

echo "Getting NOTIFY_API_KEY..."
export NOTIFY_API_KEY=$(kubectl -n create-and-vary-a-licence-api-dev get secrets create-and-vary-a-licence-api -o json | jq -r '.data.NOTIFY_API_KEY | @base64d')

# --- Booleans / Flags ---
export POLICYV3_ENABLED=true
export RECALL_ENABLED=false
export RECALL_PRISONS="MDI"
export RECALL_REGIONS="N55"


# --- Write to .env file ---
fileDir=~/env-config/
mkdir -p "$fileDir"
cd "$fileDir"
fileToAddVars='cvl.env'

echo "Writing environment variables to $fileDir$fileToAddVars..."

cat > "$fileToAddVars" <<EOF
# --- Strings ---
  SERVER_PORT=${SERVER_PORT}
  DB_SERVER=${DB_SERVER}
  DB_NAME=${DB_NAME}
  DB_USER=${DB_USER}
  DB_PASS=${DB_PASS}
  HMPPS_AUTH_URL=${HMPPS_AUTH_URL}
  SPRING_DATASOURCE_URL=${SPRING_DATASOURCE_URL}

# --- Secrets ---
  OS_PLACES_API_KEY="${OS_PLACES_API_KEY}"
  SYSTEM_CLIENT_ID="${SYSTEM_CLIENT_ID}"
  SYSTEM_CLIENT_SECRET="${SYSTEM_CLIENT_SECRET}"
  NOTIFY_API_KEY="${NOTIFY_API_KEY}"

# --- Flags ---
  POLICYV3_ENABLED=${POLICYV3_ENABLED}
  RECALL_ENABLED=${RECALL_ENABLED}

# --- Recall trial ---
  RECALL_PRISONS=${RECALL_PRISONS}
  RECALL_REGIONS=${RECALL_REGIONS}
EOF

echo "✅ Done. Environment variables saved to: $fileDir$fileToAddVars"
