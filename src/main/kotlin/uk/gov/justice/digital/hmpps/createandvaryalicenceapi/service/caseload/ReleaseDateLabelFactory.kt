package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.LocalDate

const val LABEL_FOR_CRD_RELEASE_DATE = "Conditional release date"
const val LABEL_FOR_CONFIRMED_RELEASE_DATE = "Confirmed release date"
const val LABEL_FOR_HDC_ACTUAL_DATE = "HDC actual date"
const val LABEL_FOR_HDC_ELIGIBLE_DATE = "HDC eligible date"
const val LABEL_FOR_PRRD_RELEASE_DATE = "Post-recall release date"

@Component
class ReleaseDateLabelFactory(
  private val workingDaysService: WorkingDaysService,
) {

  fun getLabel(
    releaseDate: LocalDate?,
    confirmedReleaseDate: LocalDate?,
    postRecallDate: LocalDate?,
    hdcActualDate: LocalDate?,
    hdcEligibleDate: LocalDate?,
  ): String {
    val prrdLicenceStartDate = postRecallDate?.let { workingDaysService.getLastWorkingDay(postRecallDate) }

    val label = when (releaseDate) {
      null -> LABEL_FOR_CRD_RELEASE_DATE
      confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
      prrdLicenceStartDate -> LABEL_FOR_PRRD_RELEASE_DATE
      hdcActualDate -> LABEL_FOR_HDC_ACTUAL_DATE
      hdcEligibleDate -> LABEL_FOR_HDC_ELIGIBLE_DATE
      else -> LABEL_FOR_CRD_RELEASE_DATE
    }
    return label
  }

  fun fromLicenceCase(licence: LicenceCase): String = getLabel(
    releaseDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcActualDate = licence.homeDetentionCurfewActualDate,
    hdcEligibleDate = licence.homeDetentionCurfewEligibilityDate,
  )

  fun fromLicence(licence: Licence): String = getLabel(
    releaseDate = licence.licenceStartDate,
    confirmedReleaseDate = licence.actualReleaseDate,
    postRecallDate = licence.postRecallReleaseDate,
    hdcActualDate = if (licence.kind.isHdc()) licence.homeDetentionCurfewActualDate else null,
    hdcEligibleDate = if (licence.kind.isHdc()) licence.homeDetentionCurfewEligibilityDate else null,
  )

  fun fromPrisonerSearch(licenceStartDate: LocalDate?, offender: PrisonerSearchPrisoner): String = getLabel(
    releaseDate = licenceStartDate,
    confirmedReleaseDate = offender.confirmedReleaseDate,
    postRecallDate = offender.postRecallReleaseDate,
    hdcActualDate = offender.homeDetentionCurfewActualDate,
    hdcEligibleDate = offender.homeDetentionCurfewEligibilityDate,
  )
}
