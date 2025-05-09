package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.hasChanged
import java.time.LocalDate

data class SentenceChanges(
  val lsdChanged: Boolean,
  val ledChanged: Boolean,
  val sedChanged: Boolean,
  val tussdChanged: Boolean,
  val tusedChanged: Boolean,
  val prrdChanged: Boolean,
  val hdcadChanged: Boolean,
  val hdcEndDateChanged: Boolean,
  val isMaterial: Boolean,
)

fun SentenceDetail.toSentenceDates() = SentenceDates(
  conditionalReleaseDate = conditionalReleaseOverrideDate ?: conditionalReleaseDate,
  actualReleaseDate = confirmedReleaseDate,
  sentenceStartDate = sentenceStartDate,
  sentenceEndDate = sentenceExpiryOverrideDate ?: sentenceExpiryDate,
  licenceExpiryDate = licenceExpiryOverrideDate ?: licenceExpiryDate,
  topupSupervisionStartDate = topupSupervisionStartDate,
  topupSupervisionExpiryDate = topupSupervisionExpiryOverrideDate ?: topupSupervisionExpiryDate,
  postRecallReleaseDate = postRecallReleaseOverrideDate ?: postRecallReleaseDate,
  homeDetentionCurfewEligibilityDate = homeDetentionCurfewEligibilityDate,
  homeDetentionCurfewActualDate = homeDetentionCurfewActualDate,
  homeDetentionCurfewEndDate = homeDetentionCurfewEndDate,
)

data class SentenceDates(
  val conditionalReleaseDate: LocalDate? = null,
  val actualReleaseDate: LocalDate? = null,
  val sentenceStartDate: LocalDate? = null,
  val sentenceEndDate: LocalDate? = null,
  val licenceExpiryDate: LocalDate? = null,
  val topupSupervisionStartDate: LocalDate? = null,
  val topupSupervisionExpiryDate: LocalDate? = null,
  val postRecallReleaseDate: LocalDate? = null,
  val homeDetentionCurfewActualDate: LocalDate? = null,
  val homeDetentionCurfewEndDate: LocalDate? = null,
  val homeDetentionCurfewEligibilityDate: LocalDate? = null,
)

fun Licence.getSentenceChanges(sentenceDates: SentenceDates, newLsd: LocalDate?): SentenceChanges {
  val lsdChanged = licenceStartDate.hasChanged(newLsd)
  val ledChanged = licenceExpiryDate.hasChanged(sentenceDates.licenceExpiryDate)
  val sedChanged = sentenceEndDate.hasChanged(sentenceDates.sentenceEndDate)
  val tussdChanged = topupSupervisionStartDate.hasChanged(sentenceDates.topupSupervisionStartDate)
  val tusedChanged = topupSupervisionExpiryDate.hasChanged(sentenceDates.topupSupervisionExpiryDate)
  val prrdChanged = postRecallReleaseDate.hasChanged(sentenceDates.postRecallReleaseDate)
  val hdcadChanged =
    this is HdcLicence && homeDetentionCurfewActualDate.hasChanged(sentenceDates.homeDetentionCurfewActualDate)
  val hdcEndDateChanged =
    this is HdcLicence && homeDetentionCurfewEndDate.hasChanged(sentenceDates.homeDetentionCurfewEndDate)

  val isMaterial =
    (lsdChanged || ledChanged || tussdChanged || tusedChanged || prrdChanged || hdcadChanged || hdcEndDateChanged || (sedChanged && this.statusCode == LicenceStatus.APPROVED))

  return SentenceChanges(
    lsdChanged,
    ledChanged,
    sedChanged,
    tussdChanged,
    tusedChanged,
    prrdChanged,
    hdcadChanged,
    hdcEndDateChanged,
    isMaterial,
  )
}

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
