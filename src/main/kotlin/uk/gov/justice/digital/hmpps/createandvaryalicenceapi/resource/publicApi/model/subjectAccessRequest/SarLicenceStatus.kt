package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue

enum class SarLicenceStatus(@JsonValue val description: String) {
  IN_PROGRESS("The licence is in progress and has not yet been submitted for approval"),
  SUBMITTED("The licence has been submitted for approval by the prison/ duty governor"),
  APPROVED("The licence has been approved by the prison/duty governor and is awaiting release of the PiP"),
  ACTIVE("The PiP has been released and their approved licence is now active"),
  REJECTED("Rejected, currently unused"),
  INACTIVE("The licence is soft-deleted but is kept for historic purposes"),
  RECALLED("Recalled, currently unused"),
  VARIATION_IN_PROGRESS("Variation of a licence is in progress"),
  VARIATION_SUBMITTED("Variation of a licence is submitted"),
  VARIATION_REJECTED("Variation of a licence is rejected"),
  VARIATION_APPROVED("Variation of a licence is approved"),
  NOT_STARTED("The licence is not started"),
  TIMED_OUT("The licence is timed out"),
}
