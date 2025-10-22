package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.LocalDate

const val LABEL_FOR_CRD_RELEASE_DATE = "CRD"
const val LABEL_FOR_CONFIRMED_RELEASE_DATE = "Confirmed release date"
const val LABEL_FOR_HDC_RELEASE_DATE = "HDCAD"
const val LABEL_FOR_PRRD_RELEASE_DATE = "Post-recall release date (PRRD)"

@Component
class ReleaseDateLabelFactory(
  private val workingDaysService: WorkingDaysService,
) {

  fun getLabel(
    releaseDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    postRecallDate: LocalDate?,
    hdcReleaseDate: LocalDate?,
  ): String {
    val prrdLicenceStartDate = postRecallDate?.let { workingDaysService.getLastWorkingDay(postRecallDate) }

    val label = when (releaseDate) {
      null -> LABEL_FOR_CRD_RELEASE_DATE
      confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
      prrdLicenceStartDate -> LABEL_FOR_PRRD_RELEASE_DATE
      hdcReleaseDate -> LABEL_FOR_HDC_RELEASE_DATE
      else -> LABEL_FOR_CRD_RELEASE_DATE
    }
    return label
  }

  fun fromLicenceCase(licenceCase: LicenceCase): String = getLabel(
    releaseDate = licenceCase.licenceStartDate,
    confirmedReleaseDate = licenceCase.actualReleaseDate,
    postRecallDate = licenceCase.postRecallReleaseDate,
    hdcReleaseDate = licenceCase.homeDetentionCurfewActualDate,
  )

  fun fromLicence(licence: Licence): String = getLabel(
    releaseDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcReleaseDate = if (licence.isHdcLicence()) licence.homeDetentionCurfewActualDate else null,
  )

  fun fromPrisonerSearch(licenceStartDate: LocalDate?, offender: PrisonerSearchPrisoner): String = getLabel(
    releaseDate = licenceStartDate,
    confirmedReleaseDate = offender.confirmedReleaseDate,
    postRecallDate = offender.postRecallReleaseDate,
    hdcReleaseDate = offender.homeDetentionCurfewActualDate,
  )
}
