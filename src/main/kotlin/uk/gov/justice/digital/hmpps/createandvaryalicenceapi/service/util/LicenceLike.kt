package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDateTime

interface LicenceLike {
  val kind: LicenceKind
  val statusCode: LicenceStatus

  fun isHdcLicence(): Boolean = kind.isVariation()

  fun isVariation() = kind.isVariation()
}

interface HasReviewDate : LicenceLike {
  val reviewDate: LocalDateTime?

  fun isReviewNeeded() = statusCode == ACTIVE && reviewDate == null
}
