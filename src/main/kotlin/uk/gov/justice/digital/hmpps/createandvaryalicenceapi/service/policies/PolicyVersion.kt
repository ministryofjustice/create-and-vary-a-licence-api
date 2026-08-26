package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

enum class PolicyVersion(val version: String) {
  V1_0("1.0"),
  V2_0("2.0"),
  V2_1("2.1"),
  V3_0("3.0"),
  V4_0("4.0"),
}

val PRE_PROGRESSION_POLICY_VERSIONS = listOf(
  PolicyVersion.V1_0.version,
  PolicyVersion.V2_0.version,
  PolicyVersion.V2_1.version,
  PolicyVersion.V3_0.version,
)
