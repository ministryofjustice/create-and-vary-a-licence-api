#!/usr/bin/env bash

# Script to output the standard and additional conditions for a given policy.
# Requires: jq

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
ROOT="$(cd -- "${SCRIPT_DIR}/.." && pwd)"
POLICY_DIR="${ROOT}/src/test/resources/test_data/policy_conditions"

VERSION="${1:-v4}"
OUTPUT_CSV="policy-${VERSION}-conditions.csv"

case "${VERSION}" in
  v1)   INPUT_JSON="${POLICY_DIR}/policyV1.json" ;;
  v2)   INPUT_JSON="${POLICY_DIR}/policyV2.json" ;;
  v2.1) INPUT_JSON="${POLICY_DIR}/policyV2_1.json" ;;
  v3)   INPUT_JSON="${POLICY_DIR}/policyV3.json" ;;
  v4)   INPUT_JSON="${POLICY_DIR}/policyV4.json" ;;
  *)
    echo "Error: version must be one of: v1, v2, v2.1, v3, v4" >&2
    exit 1
    ;;
esac

if ! command -v jq >/dev/null 2>&1; then
  echo "Error: jq is required but not installed." >&2
  exit 1
fi

if [[ ! -f "$INPUT_JSON" ]]; then
  echo "Error: input file not found: $INPUT_JSON" >&2
  exit 1
fi

echo "=== Outputting standard and additional conditions for policy version ${VERSION} ==="

{
  echo '"CVL Code","CVL Main Heading","CVL Detail"'
  jq -r '
    (
      .standardConditions.AP[]
      | [.code, "Standard Condition", .text]
    ),
    (
      .additionalConditions.AP[]
      | [ .code, .category, .text ]
    )
    | @csv
  ' "$INPUT_JSON"
} > "$OUTPUT_CSV"

echo "CSV created: $OUTPUT_CSV"
