package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS

enum class LicenceKind(
  val initialStatus: LicenceStatus,
  val copyEventType: LicenceEventType,
  val submittedEventType: LicenceEventType,
) {
  CRD(
    IN_PROGRESS,
    LicenceEventType.VERSION_CREATED,
    LicenceEventType.SUBMITTED,
  ),

  VARIATION(
    VARIATION_IN_PROGRESS,
    LicenceEventType.VARIATION_CREATED,
    LicenceEventType.VARIATION_SUBMITTED,
  ),

  HARDSTOP(
    IN_PROGRESS,
    LicenceEventType.HARDSTOP_CREATED,
    LicenceEventType.HARDSTOP_SUBMITTED,
  ),
}
