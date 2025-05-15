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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.hasChanged
import java.time.LocalDate

fun Licence.getDateChanges(sentenceDates: SentenceDates, newLsd: LocalDate?): DateChanges {
  val dateChanges = listOf(
    DateChange(LSD, newLsd, licenceStartDate),
    DateChange(CRD, sentenceDates.conditionalReleaseDate, conditionalReleaseDate),
    DateChange(ARD, sentenceDates.actualReleaseDate, actualReleaseDate),
    DateChange(LED, sentenceDates.licenceExpiryDate, licenceExpiryDate),
    DateChange(SSD, sentenceDates.sentenceStartDate, sentenceStartDate),
    DateChange(SED, sentenceDates.sentenceEndDate, sentenceEndDate),
    DateChange(TUSSD, sentenceDates.topupSupervisionStartDate, topupSupervisionStartDate),
    DateChange(TUSED, sentenceDates.topupSupervisionExpiryDate, topupSupervisionExpiryDate),
    DateChange(PRRD, sentenceDates.postRecallReleaseDate, this.postRecallReleaseDate),
    DateChange(
      HDCAD,
      if (this is HdcLicence) sentenceDates.homeDetentionCurfewActualDate else null,
      if (this is HdcLicence) homeDetentionCurfewActualDate else null,
    ),
    DateChange(
      HDCENDDATE,
      if (this is HdcLicence) sentenceDates.homeDetentionCurfewEndDate else null,
      if (this is HdcLicence) homeDetentionCurfewEndDate else null,
    ),
  )

  val notifyDueToSedChange = sentenceEndDate.hasChanged(sentenceDates.sentenceEndDate) && this.statusCode == APPROVED

  val isMaterial = notifyDueToSedChange || dateChanges.any { it.denotesMaterialChange(kind) }

  return DateChanges(dateChanges, isMaterial)
}
