package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.subjectAccessRequest

import com.fasterxml.jackson.annotation.JsonValue
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

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
  ;

  companion object {
    fun from(type: LicenceStatus) = when (type) {
      LicenceStatus.IN_PROGRESS -> IN_PROGRESS
      LicenceStatus.SUBMITTED -> SUBMITTED
      LicenceStatus.APPROVED -> APPROVED
      LicenceStatus.ACTIVE -> ACTIVE
      LicenceStatus.REJECTED -> REJECTED
      LicenceStatus.INACTIVE -> INACTIVE
      LicenceStatus.RECALLED -> RECALLED
      LicenceStatus.VARIATION_IN_PROGRESS -> VARIATION_IN_PROGRESS
      LicenceStatus.VARIATION_SUBMITTED -> VARIATION_SUBMITTED
      LicenceStatus.VARIATION_REJECTED -> VARIATION_REJECTED
      LicenceStatus.VARIATION_APPROVED -> VARIATION_APPROVED
      LicenceStatus.NOT_STARTED -> NOT_STARTED
      LicenceStatus.TIMED_OUT -> TIMED_OUT
    }
  }
}
