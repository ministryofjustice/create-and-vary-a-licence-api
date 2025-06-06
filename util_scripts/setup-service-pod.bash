#!/bin/bash
namespace=create-and-vary-a-licence-api
#
# The scripts allow us to create and run q pod locally we can then run commands in pod to get various information
# In CVL this was used to get DB upgrade data using aws commands e.g.
# aws rds describe-db-instances --db-instance-identifier "$RDS_INSTANCE_IDENTIFIER" --query 'DBInstances[0].EngineVersion' --output text
# aws rds describe-db-engine-versions --engine postgres --engine-version 15.7 --query "DBEngineVersions[*].ValidUpgradeTarget[*].{EngineVersion:EngineVersion}" --output text
#
usage() {
  echo
  echo "Usage:"
  echo
  echo " command line parameters:"
  echo
  echo "   -ns <namespace>  One of 'dev', 'preprod' or 'prod'. Selects the kubernetes namespace. "
  echo
  exit
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
      exit
      ;;
    esac
    shift
  done
}

check_namespace() {
  case "$NS_KEY" in
  dev | preprod | prod)
    namespace=${namespace}-${NS_KEY}
    ;;
  *)
    echo "-ns must be 'dev', 'preprod' or 'prod'"
    exit
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
echo "service pod name: $debug_pod_name"
service_pod_exists="$(kubectl --namespace="$namespace" get pods "$debug_pod_name" || echo 'NotFound')"

if [[ ! $service_pod_exists =~ 'NotFound' ]]; then
  echo "$debug_pod_name exists signing into shell"
  kubectl exec -it -n "$namespace" "$debug_pod_name" -- sh
  exit 0
fi

# Get credentials such as RDS identifiers from namespace secrets
echo "🔑 Getting RDS instance from secrets ... $namespace"
secret_json=$(cloud-platform decode-secret -s rds-instance-output -n "$namespace" --skip-version-check)
echo "Secret for RDS instance $secret_json"

command=$(echo "$secret_json" | jq -r .data.rds_instance_address | sed s/[.].*//)
echo "RDS_INSTANCE_IDENTIFIER $command, now set"
export RDS_INSTANCE_IDENTIFIER=$command
export DB_DATA=$(kubectl -n create-and-vary-a-licence-api-dev get secrets rds-instance-output -o json)
export DB_PASSWORD=$(echo $DB_DATA | jq -r '.data.database_password | @base64d')
export DB_NAME=$(echo $DB_DATA | jq -r '.data.database_name | @base64d')
export DB_USER=$(echo $DB_DATA | jq -r '.data.database_username | @base64d')
export DB_SERVER=$(echo $DB_DATA | jq -r '.data.rds_instance_address | @base64d')

kubectl --namespace=$namespace --request-timeout='120s' run \
     --env "namespace=$namespace" \
     --env "RDS_INSTANCE_IDENTIFIER=$RDS_INSTANCE_IDENTIFIER" \
     --env "DB_SERVER=$DB_SERVER"  \
     --env "DB_NAME=$DB_NAME"  \
     --env "DB_USER=$DB_USER"  \
     --env "DB_PASS=$DB_PASSWORD" \
     -it --rm $debug_pod_name --image=quay.io/hmpps/hmpps-probation-in-court-utils:latest \
     --restart=Never --overrides='{ "spec": { "serviceAccount": "create-and-vary-a-licence-api" } }'
