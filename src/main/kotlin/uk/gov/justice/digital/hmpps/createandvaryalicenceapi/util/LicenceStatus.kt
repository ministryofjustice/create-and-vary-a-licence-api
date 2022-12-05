package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceStatus {
  IN_PROGRESS,
  SUBMITTED,
  APPROVED,
  ACTIVE,
  REJECTED,
  INACTIVE,
  RECALLED,
  VARIATION_IN_PROGRESS,
  VARIATION_SUBMITTED,
  VARIATION_REJECTED,
  VARIATION_APPROVED;

  companion object {
    fun lookupLicenceEventByStatus(status: LicenceStatus): LicenceEventType {
      return when (status) {
        SUBMITTED -> LicenceEventType.SUBMITTED
        IN_PROGRESS -> LicenceEventType.BACK_IN_PROGRESS
        APPROVED -> LicenceEventType.APPROVED
        ACTIVE -> LicenceEventType.ACTIVATED
        REJECTED -> LicenceEventType.VARIATION_REFERRED
        VARIATION_IN_PROGRESS -> LicenceEventType.VARIATION_IN_PROGRESS
        VARIATION_SUBMITTED -> LicenceEventType.VARIATION_SUBMITTED
        VARIATION_REJECTED -> LicenceEventType.VARIATION_REFERRED
        VARIATION_APPROVED -> LicenceEventType.VARIATION_APPROVED
        INACTIVE -> LicenceEventType.INACTIVE
        RECALLED -> LicenceEventType.RECALLED
      }
    }
  }
}
