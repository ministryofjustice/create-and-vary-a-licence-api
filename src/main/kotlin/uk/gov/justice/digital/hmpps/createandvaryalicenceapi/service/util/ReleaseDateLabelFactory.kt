package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.LocalDate

const val LABEL_FOR_CRD_RELEASE_DATE = "CRD"
const val LABEL_FOR_CONFIRMED_RELEASE_DATE = "Confirmed release date"
const val LABEL_FOR_HDC_RELEASE_DATE = "HDCAD"
const val LABEL_FOR_PRRD_RELEASE_DATE = "Post-recall release date (PRRD)"

@Service
class ReleaseDateLabelFactory(
  private val workingDaysService: WorkingDaysService,
) {

  fun getLabel(
    licenceStartDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    postRecallDate: LocalDate?,
    hdcReleaseDate: LocalDate?,
  ): String {
    val prrdLicenceStartDate = postRecallDate?.let { workingDaysService.getLastWorkingDay(postRecallDate) }

    val label = when (licenceStartDate) {
      null -> LABEL_FOR_CRD_RELEASE_DATE
      confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
      prrdLicenceStartDate -> LABEL_FOR_PRRD_RELEASE_DATE
      hdcReleaseDate -> LABEL_FOR_HDC_RELEASE_DATE
      else -> LABEL_FOR_CRD_RELEASE_DATE
    }
    return label
  }

  fun fromLicenceSummary(licence: LicenceSummary): String = getLabel(
    licenceStartDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcReleaseDate = licence.homeDetentionCurfewActualDate,
  )

  fun fromLicence(licence: Licence): String = getLabel(
    licenceStartDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcReleaseDate = if (licence.isHdcLicence()) licence.homeDetentionCurfewActualDate else null,
  )

  fun fromPrisoner(releaseDate: LocalDate?, nomis: Prisoner): String = getLabel(
    licenceStartDate = releaseDate,
    confirmedReleaseDate = nomis.confirmedReleaseDate,
    postRecallDate = nomis.postRecallReleaseDate,
    hdcReleaseDate = nomis.homeDetentionCurfewActualDate,
  )

  fun fromPrisonerSearch(licenceStartDate: LocalDate?, offender: PrisonerSearchPrisoner): String = getLabel(
    licenceStartDate = licenceStartDate,
    confirmedReleaseDate = offender.confirmedReleaseDate,
    postRecallDate = offender.postRecallReleaseDate,
    hdcReleaseDate = offender.homeDetentionCurfewActualDate,
  )
}
