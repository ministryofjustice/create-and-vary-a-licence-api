package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.determineReleaseDateKind

@Service
class CaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
) {

  fun getPrisonersByNumber(nomisIds: List<String>): List<CaseloadItem> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds)

    val licenceStartDates =
      releaseDateService.getLicenceStartDates(prisoners)

    return prisoners.map {
      CaseloadItem(
        prisoner = it.toPrisoner(),
        licenceStartDate = licenceStartDates[it.prisonerNumber],
      )
    }
  }

  fun getPrisoner(nomisId: String): PrisonerWithCvlFields {
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)
    val licenceKind = determineReleaseDateKind(prisoner.postRecallReleaseDate, prisoner.conditionalReleaseDate)
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisoner, licenceKind)
    val sentenceDateHolder = prisoner.toSentenceDateHolder(licenceStartDate)

    return PrisonerWithCvlFields(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(prisoner),
        hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder.licenceStartDate),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder.licenceStartDate),
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(sentenceDateHolder),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        licenceStartDate = licenceStartDate,
      ),
    )
  }
}
