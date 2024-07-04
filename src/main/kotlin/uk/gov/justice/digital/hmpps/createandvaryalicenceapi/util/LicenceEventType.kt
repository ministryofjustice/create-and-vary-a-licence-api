package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

enum class LicenceEventType {
  CREATED,
  SUBMITTED,
  BACK_IN_PROGRESS,
  APPROVED,
  ACTIVATED,
  SUPERSEDED,
  HARD_STOP_CREATED,
  HARD_STOP_SUBMITTED,
  HARD_STOP_REVIEWED_WITHOUT_VARIATION,
  HARD_STOP_REVIEWED_WITH_VARIATION,
  VARIATION_CREATED,
  VARIATION_SUBMITTED_REASON,
  VARIATION_IN_PROGRESS,
  VARIATION_SUBMITTED,
  VARIATION_REFERRED,
  VARIATION_APPROVED,
  INACTIVE,
  RECALLED,
  VERSION_CREATED,
  NOT_STARTED,
  TIMED_OUT,
  OOS_BOTUS,
  OOS_RECALL,
}
