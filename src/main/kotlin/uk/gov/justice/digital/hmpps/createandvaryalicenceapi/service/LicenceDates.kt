package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.ARD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.HDCAD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.HDCENDDATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.LED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.LSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.SED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.SSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.TUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceDateType.TUSSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.hasChanged
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

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

enum class LicenceDateType(val description: String, val hdcOnly: Boolean = false, val notifyOnChange: Boolean = true) {
  LSD("Release date"),
  CRD("Conditional release data", notifyOnChange = false),
  ARD("Confirmed release data", notifyOnChange = false),
  LED("Licence end date"),
  SSD("Sentence start date", notifyOnChange = false),
  SED("Sentence end date"),
  TUSSD("Top up supervision start date"),
  TUSED("Top up supervision end date"),
  PRRD("Post recall release date"),
  HDCAD("HDC actual date", hdcOnly = true),
  HDCENDDATE("HDC end date", hdcOnly = true),
}

data class DateChange(
  val type: LicenceDateType,
  val changed: Boolean,
  val newDate: LocalDate?,
  val oldDate: LocalDate?,
) {
  fun toDescription() = "${type.description} has changed to ${newDate?.format(dateFormat)}"
}

data class SentenceChanges(
  val dates: List<DateChange>,
  val isMaterial: Boolean,
) {
  fun toChanges(kind: LicenceKind) = dates
    .filter { (kind.isHdc() || !it.type.hdcOnly) && it.changed }
    .associate {
      it.type.name to mapOf("from" to it.oldDate?.toString(), "to" to it.newDate?.toString())
    }
}

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
