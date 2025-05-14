package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

/**
 * Update licence status in response to sentence change.
 * ACTIVE licences can be made INACTIVE when release dates are moved to a future date e.g. offender is recalled
 */
fun Licence.calculateStatusCode(sentenceDates: SentenceDates): LicenceStatus {
  val now = LocalDate.now()
  return when {
    this.statusCode == LicenceStatus.ACTIVE &&
      (
        sentenceDates.actualReleaseDate?.isAfter(now) == true ||
          sentenceDates.conditionalReleaseDate?.isAfter(now) == true
        ) -> LicenceStatus.INACTIVE

    this.statusCode == LicenceStatus.ACTIVE &&
      this.typeCode != LicenceType.PSS &&
      sentenceDates.postRecallReleaseDate?.isAfter(now) == true -> LicenceStatus.INACTIVE

    else -> this.statusCode
  }
}
