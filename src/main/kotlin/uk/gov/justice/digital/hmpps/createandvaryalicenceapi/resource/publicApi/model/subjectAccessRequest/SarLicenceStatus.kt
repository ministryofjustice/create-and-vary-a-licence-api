package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarLicenceStatus(@JsonValue val description: String) {
  IN_PROGRESS("In progress"),
  SUBMITTED("Submitted"),
  APPROVED("Approved"),
  ACTIVE("Active"),
  REJECTED("Rejected"),
  INACTIVE("Inactive"),
  RECALLED("Recalled"),
  VARIATION_IN_PROGRESS("Variation in progress"),
  VARIATION_SUBMITTED("Variation submitted"),
  VARIATION_REJECTED("Variation rejected"),
  VARIATION_APPROVED("Variation approved"),
  NOT_STARTED("Not started"),
  TIMED_OUT("Timed out"),
}
