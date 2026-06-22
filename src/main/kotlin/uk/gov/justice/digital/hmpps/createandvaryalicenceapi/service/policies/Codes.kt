package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.Version.V1
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.Version.V2

const val ELECTRONIC_TAG_COND_CODE_14A = "fd129172-bdd3-4d97-a4a0-efd7b47a49d4"
const val ELECTRONIC_TAG_COND_CODE_14B = "524f2fd6-ad53-47dd-8edc-2161d3dd2ed4"
const val ELECTRONIC_TAG_COND_CODE_14C = "86e6f2a9-bb60-40f8-9ac4-310ebc72ac2f"
const val ELECTRONIC_TAG_COND_CODE_14D = "d36a3b77-30ba-40ce-8953-83e761d3b487"
const val ELECTRONIC_TAG_COND_CODE_14E = "2F8A5418-C6E4-4F32-9E58-64B23550E504"
const val MULTIPLE_UPLOAD_COND_CODE = "005d70e4-a247-4f82-b8b3-6d294a0f5051"
const val EXCLUSION_ZONE_COND_CODE = "0f9a20f4-35c7-4c77-8af8-f200f153fa11"
const val EVENT_EXCLUSION_COND_CODE = "99195049-f355-46fb-b7d8-aef87a1b19c5"

enum class Version {
  V1,
  V2,
}

enum class ElectronicMonitoringType(val text: Map<Version, String?>) {
  EXCLUSION_ZONE(
    V1 to "exclusion zone",
    V2 to "that you do not go to areas you must not enter (exclusion zones)",
  ),

  CURFEW(
    V1 to "curfew",
    V2 to "that you comply with your curfew",
  ),

  LOCATION_MONITORING(
    V1 to "location monitoring",
    V2 to "your location",
  ),

  ATTENDANCE_AT_APPOINTMENTS(
    V1 to "attendance at appointments",
    V2 to "that you attend appointments",
  ),

  ALCOHOL_MONITORING(
    V1 to "alcohol monitoring",
    V2 to "that you do not drink any alcohol",
  ),

  ALCOHOL_ABSTINENCE(
    V1 to "alcohol abstinence",
    V2 to "your alcohol consumption",
  ),

  RESTRICTION_ZONE(
    V1 to null,
    V2 to "that you do not leave areas you must stay in (restriction zones)",
  ),
  ;

  constructor(vararg values: Pair<Version, String?>) : this(values.toMap())

  companion object {
    fun find(value: String): ElectronicMonitoringType? = ElectronicMonitoringType.entries.find { it.text.values.contains(value) }
  }
}
