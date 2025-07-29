#!/bin/bash

#
# This script creates a debug pod in a given CVL environment to run AWS or DB commands interactively.
#

usage() {
  echo
  echo "Usage:"
  echo
  echo "   -ns <namespace>  One of: 'dev', 'test1', 'test2', 'preprod', 'prod'"
  echo
  exit 1
}

read_command_line() {
  if [[ ! $1 ]]; then
    usage
  fi
  while [[ $1 ]]; do
    case $1 in
    -ns)
      shift
      NS_KEY=$1
      ;;
    *)
      echo
      echo "Unknown argument '$1'"
      echo
      exit 1
      ;;
    esac
    shift
  done
}

check_namespace() {
  case "$NS_KEY" in
  dev | preprod | prod)
    base=create-and-vary-a-licence-api
    namespace=${base}-${NS_KEY}

    ;;
  test1 | test2)
    base=create-and-vary-a-licence
    namespace=${base}-${NS_KEY}
    ;;
  *)
    echo "-ns must be one of: 'dev', 'test1', 'test2', 'preprod', 'prod'"
    exit 1
    ;;
  esac
}

read_command_line "$@"
check_namespace

set -o history -o histexpand
set -e

exit_on_error() {
  exit_code=$1
  last_command=${@:2}
  if [ $exit_code -ne 0 ]; then
    >&2 echo "💥 Last command:"
    >&2 echo "    \"${last_command}\""
    >&2 echo "❌ Failed with exit code ${exit_code}."
    >&2 echo "🟥 Aborting"
    exit "$exit_code"
  fi
}

debug_pod_name=service-pod-$namespace
echo "🔍 Service pod name: $debug_pod_name"
service_pod_exists="$(kubectl --namespace="$namespace" get pods "$debug_pod_name" || echo 'NotFound')"

if [[ ! $service_pod_exists =~ 'NotFound' ]]; then
  echo "$debug_pod_name exists, signing into shell..."
  kubectl exec -it -n "$namespace" "$debug_pod_name" -- sh
  exit 0
fi

# 🔐 Get credentials from secrets
echo "🔑 Fetching RDS instance secrets from $namespace..."
secret_json=$(cloud-platform decode-secret -s rds-instance-output -n "$namespace" --skip-version-check)
echo "🔓 Secret for RDS instance: $secret_json"

command=$(echo "$secret_json" | jq -r .data.rds_instance_address | sed s/[.].*//)
echo "📡 RDS_INSTANCE_IDENTIFIER set to $command"
export RDS_INSTANCE_IDENTIFIER=$command

export DB_DATA=$(kubectl -n "$namespace" get secrets rds-instance-output -o json)
export DB_PASSWORD=$(echo $DB_DATA | jq -r '.data.database_password | @base64d')
export DB_NAME=$(echo $DB_DATA | jq -r '.data.database_name | @base64d')
export DB_USER=$(echo $DB_DATA | jq -r '.data.database_username | @base64d')
export DB_SERVER=$(echo $DB_DATA | jq -r '.data.rds_instance_address | @base64d')

kubectl --namespace="$namespace" --request-timeout='120s' run \
  --env "namespace=$namespace" \
  --env "RDS_INSTANCE_IDENTIFIER=$RDS_INSTANCE_IDENTIFIER" \
  --env "DB_SERVER=$DB_SERVER" \
  --env "DB_NAME=$DB_NAME" \
  --env "DB_USER=$DB_USER" \
  --env "DB_PASS=$DB_PASSWORD" \
  -it --rm "$debug_pod_name" --image=quay.io/hmpps/hmpps-probation-in-court-utils:latest \
  --restart=Never --overrides='{ "spec": { "serviceAccount": "'${base}'" } }'
