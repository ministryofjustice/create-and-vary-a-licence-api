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
  OOS_RECALL,
  OOS_BOTUS,
  NOT_IN_PILOT,
  ;

  fun isOnProbation() = ON_PROBATION_STATUSES.contains(this)

  companion object {
    fun lookupLicenceEventByStatus(status: LicenceStatus): LicenceEventType {
      return when (status) {
        IN_PROGRESS -> LicenceEventType.BACK_IN_PROGRESS
        ACTIVE -> LicenceEventType.ACTIVATED
        REJECTED -> LicenceEventType.VARIATION_REFERRED
        VARIATION_REJECTED -> LicenceEventType.VARIATION_REFERRED
        RECALLED -> LicenceEventType.RECALLED
        else -> LicenceEventType.valueOf(status.toString())
      }
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

    val DRAFT_LICENCES = listOf(
      OOS_RECALL,
      OOS_BOTUS,
      NOT_IN_PILOT,
      NOT_STARTED,
      IN_PROGRESS,
      SUBMITTED,
      APPROVED,
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
