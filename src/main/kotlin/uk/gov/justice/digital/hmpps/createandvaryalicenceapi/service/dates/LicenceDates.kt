package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.ARD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.HDCAD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.HDCENDDATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.LED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.LSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.SED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.SSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.TUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.TUSSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.hasChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

fun Licence.getDateChanges(sentenceDates: SentenceDates, newLsd: LocalDate?): SentenceChanges {
  val lsdChanged = licenceStartDate.hasChanged(newLsd)
  val crdChanged = conditionalReleaseDate.hasChanged(sentenceDates.conditionalReleaseDate)
  val ardChanged = actualReleaseDate.hasChanged(sentenceDates.actualReleaseDate)
  val ssdChanged = sentenceStartDate.hasChanged(sentenceDates.sentenceStartDate)
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
    listOf(
      DateChange(LSD, lsdChanged, newLsd, licenceStartDate),
      DateChange(CRD, crdChanged, sentenceDates.conditionalReleaseDate, conditionalReleaseDate),
      DateChange(ARD, ardChanged, sentenceDates.actualReleaseDate, actualReleaseDate),
      DateChange(LED, ledChanged, sentenceDates.licenceExpiryDate, licenceExpiryDate),
      DateChange(SSD, ssdChanged, sentenceDates.sentenceStartDate, sentenceStartDate),
      DateChange(SED, sedChanged, sentenceDates.sentenceEndDate, sentenceEndDate),
      DateChange(TUSSD, tussdChanged, sentenceDates.topupSupervisionStartDate, topupSupervisionStartDate),
      DateChange(TUSED, tusedChanged, sentenceDates.topupSupervisionExpiryDate, topupSupervisionExpiryDate),
      DateChange(PRRD, prrdChanged, sentenceDates.postRecallReleaseDate, this.postRecallReleaseDate),
      DateChange(
        HDCAD,
        hdcadChanged,
        sentenceDates.homeDetentionCurfewActualDate,
        if (this is HdcLicence) homeDetentionCurfewActualDate else null,
      ),
      DateChange(
        HDCENDDATE,
        hdcEndDateChanged,
        sentenceDates.homeDetentionCurfewEndDate,
        if (this is HdcLicence) homeDetentionCurfewEndDate else null,
      ),
    ),
    isMaterial,
  )
}
