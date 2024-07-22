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
  VARIATION_APPROVED,
  NOT_STARTED,
  TIMED_OUT,
  OOS_BOTUS,
  OOS_RECALL,
  REVIEW_NEEDED,
  ;

  fun isOnProbation() = ON_PROBATION_STATUSES.contains(this)

  companion object {
    fun lookupLicenceEventByStatus(status: LicenceStatus): LicenceEventType = when (status) {
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
      NOT_STARTED -> LicenceEventType.NOT_STARTED
      TIMED_OUT -> LicenceEventType.TIMED_OUT
      OOS_BOTUS -> LicenceEventType.OOS_BOTUS
      OOS_RECALL -> LicenceEventType.OOS_RECALL
      REVIEW_NEEDED -> LicenceEventType.REVIEW_NEEDED
    }

    val IN_FLIGHT_LICENCES = listOf(
      ACTIVE,
      IN_PROGRESS,
      SUBMITTED,
      APPROVED,
      VARIATION_IN_PROGRESS,
      VARIATION_SUBMITTED,
      VARIATION_APPROVED,
      VARIATION_REJECTED,
    )

    val ON_PROBATION_STATUSES = setOf(
      ACTIVE,
      VARIATION_IN_PROGRESS,
      VARIATION_SUBMITTED,
      VARIATION_REJECTED,
      VARIATION_APPROVED,
    )
  }
}
