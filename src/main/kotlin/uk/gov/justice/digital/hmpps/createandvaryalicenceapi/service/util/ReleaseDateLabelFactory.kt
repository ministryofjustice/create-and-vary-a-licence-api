package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate

object ReleaseDateLabelFactory {

  private const val LABEL_FOR_CRD_RELEASE_DATE = "CRD"
  private const val LABEL_FOR_CONFIRMED_RELEASE_DATE = "Confirmed release date"
  private const val LABEL_FOR_HDC_RELEASE_DATE = "HDCAD"
  private const val LABEL_FOR_PRRD_RELEASE_DATE = "Post-recall release date (PRRD)"

  @JvmStatic
  fun getLabel(
    releaseDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    postRecallDate: LocalDate?,
    hdcReleaseDate: LocalDate?,
  ): String = when (releaseDate) {
    null -> LABEL_FOR_CRD_RELEASE_DATE
    confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
    postRecallDate -> LABEL_FOR_PRRD_RELEASE_DATE
    hdcReleaseDate -> LABEL_FOR_HDC_RELEASE_DATE
    else -> LABEL_FOR_CRD_RELEASE_DATE
  }

  @JvmStatic
  fun fromLicenceSummary(licence: LicenceSummary): String = getLabel(
    releaseDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcReleaseDate = licence.homeDetentionCurfewActualDate,
  )

  fun fromLicence(licence: Licence): String = getLabel(
    releaseDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcReleaseDate = if (licence.isHdcLicence()) licence.homeDetentionCurfewActualDate else null,
  )

  @JvmStatic
  fun fromPrisoner(releaseDate: LocalDate?, nomis: Prisoner): String = getLabel(
    releaseDate = releaseDate,
    confirmedReleaseDate = nomis.confirmedReleaseDate,
    postRecallDate = nomis.postRecallReleaseDate,
    hdcReleaseDate = nomis.homeDetentionCurfewActualDate,
  )

  @JvmStatic
  fun fromPrisonerSearch(licenceStartDate: LocalDate?, offender: PrisonerSearchPrisoner): String = getLabel(
    releaseDate = licenceStartDate,
    confirmedReleaseDate = offender.confirmedReleaseDate,
    postRecallDate = offender.postRecallReleaseDate,
    hdcReleaseDate = offender.homeDetentionCurfewActualDate,
  )
}
