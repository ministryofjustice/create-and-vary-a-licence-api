package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarLicenceEventType(@JsonValue val description: String) {
  CREATED("Created"),
  SUBMITTED("Submitted"),
  BACK_IN_PROGRESS("Back in progress"),
  APPROVED("Approved"),
  ACTIVATED("Activated"),
  SUPERSEDED("Superseded"),
  HARD_STOP_CREATED("Hard stop created"),
  HARD_STOP_SUBMITTED("Hard stop submitted"),
  HARD_STOP_REVIEWED_WITHOUT_VARIATION("Hard stop reviewed without variation"),
  HARD_STOP_REVIEWED_WITH_VARIATION("Hard stop reviewed with variation"),
  VARIATION_CREATED("Variation created"),
  VARIATION_SUBMITTED_REASON("Variation submitted reason"),
  VARIATION_IN_PROGRESS("Variation in progress"),
  VARIATION_SUBMITTED("Variation submitted"),
  VARIATION_REFERRED("Variation referred"),
  VARIATION_APPROVED("Variation approved"),
  INACTIVE("Inactive"),
  RECALLED("Recalled"),
  VERSION_CREATED("Version created"),
  NOT_STARTED("Not started"),
  TIMED_OUT("Timed out"),
}
