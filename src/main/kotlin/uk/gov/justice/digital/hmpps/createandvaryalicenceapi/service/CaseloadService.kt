package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Service
class CaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
) {

  fun getPrisonersByNumber(nomisIds: List<String>): List<CaseloadItem> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds)

    val licenceStartDates =
      releaseDateService.getLicenceStartDates(prisoners)

    return prisoners.map { prisonerToCaseloadItem(it, licenceStartDates[it.prisonerNumber]) }
  }

  fun getPrisonersByReleaseDate(
    earliestReleaseDate: LocalDate,
    latestReleaseDate: LocalDate,
    prisonIds: Set<String>,
    page: Int = 0,
  ): Page<CaseloadItem> {
    val prisoners =
      prisonerSearchApiClient.searchPrisonersByReleaseDate(earliestReleaseDate, latestReleaseDate, prisonIds, page)
    val licenceStartDates = releaseDateService.getLicenceStartDates(prisoners.mapNotNull { it })
    return prisoners.map {
      val licenceStartDate = licenceStartDates[it.prisonerNumber]
      prisonerToCaseloadItem(it, licenceStartDate)
    }
  }

  fun getPrisoner(nomisId: String) = getPrisonersByNumber(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)

  fun prisonerToCaseloadItem(prisoner: PrisonerSearchPrisoner, licenceStartDate: LocalDate?): CaseloadItem {
    val sentenceDateHolder = prisoner.toSentenceDateHolder(licenceStartDate)
    return CaseloadItem(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(prisoner),
        hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder),
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(sentenceDateHolder),
        isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(sentenceDateHolder),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        licenceStartDate = sentenceDateHolder.licenceStartDate,
      ),
    )
  }
}
