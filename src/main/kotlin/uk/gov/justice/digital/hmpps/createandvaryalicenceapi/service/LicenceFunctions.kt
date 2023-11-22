package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

data class SentenceChanges(
  val lsdChanged: Boolean,
  val ledChanged: Boolean,
  val sedChanged: Boolean,
  val tussdChanged: Boolean,
  val tusedChanged: Boolean,
  val pedChanged: Boolean,
  val npdChanged: Boolean,
  val prrdChanged: Boolean,
  val isMaterial: Boolean,
)

fun Licence.getSentenceChanges(newSentence: UpdateSentenceDatesRequest): SentenceChanges {
  val lsdChanged = nullableDatesDiffer(newSentence.licenceStartDate, this.licenceStartDate)
  val ledChanged = nullableDatesDiffer(newSentence.licenceExpiryDate, this.licenceExpiryDate)
  val sedChanged = nullableDatesDiffer(newSentence.sentenceEndDate, this.sentenceEndDate)
  val tussdChanged =
    nullableDatesDiffer(newSentence.topupSupervisionStartDate, this.topupSupervisionStartDate)
  val tusedChanged =
    nullableDatesDiffer(newSentence.topupSupervisionExpiryDate, this.topupSupervisionExpiryDate)
  val pedChanged = nullableDatesDiffer(newSentence.paroleEligibilityDate, this.paroleEligibilityDate)
  val npdChanged = nullableDatesDiffer(newSentence.nonParoleDate, this.nonParoleDate)
  val prrdChanged = nullableDatesDiffer(newSentence.postRecallReleaseDate, this.postRecallReleaseDate)

  val isMaterial =
    (lsdChanged || ledChanged || tussdChanged || tusedChanged || pedChanged || npdChanged || prrdChanged || (sedChanged && this.statusCode == LicenceStatus.APPROVED))

  return SentenceChanges(
    lsdChanged,
    ledChanged,
    sedChanged,
    tussdChanged,
    tusedChanged,
    pedChanged,
    npdChanged,
    prrdChanged,
    isMaterial,
  )
}

/**
 * Update licence status in response to sentence change.
 * ACTIVE licences can be made INACTIVE when release dates are moved to a future date e.g. offender is recalled
 */
fun Licence.calculateStatusCode(newSentence: UpdateSentenceDatesRequest): LicenceStatus {
  val now = LocalDate.now()
  return when {
    this.statusCode == LicenceStatus.ACTIVE && (
      newSentence.actualReleaseDate?.isAfter(now) == true ||
        newSentence.conditionalReleaseDate?.isAfter(now) == true
      ) -> LicenceStatus.INACTIVE

    this.statusCode == LicenceStatus.ACTIVE && this.typeCode != LicenceType.PSS &&
      newSentence.postRecallReleaseDate?.isAfter(now) == true -> LicenceStatus.INACTIVE

    else -> this.statusCode
  }
}
