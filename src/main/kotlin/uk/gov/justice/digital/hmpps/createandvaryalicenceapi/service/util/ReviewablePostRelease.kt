package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDateTime

interface ReviewablePostRelease {
  val kind: LicenceKind
  val reviewDate: LocalDateTime?
  val statusCode: LicenceStatus

  fun isReviewNeeded() = kind.isCreatedByPrison() && statusCode == ACTIVE && reviewDate == null
}
