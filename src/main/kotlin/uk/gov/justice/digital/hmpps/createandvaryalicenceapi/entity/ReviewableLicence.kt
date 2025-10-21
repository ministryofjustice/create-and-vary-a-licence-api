package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDateTime

interface ReviewableLicence {
  val statusCode: LicenceStatus
  val reviewDate: LocalDateTime?

  fun isReviewNeeded(): Boolean = statusCode == LicenceStatus.ACTIVE && reviewDate == null
}
