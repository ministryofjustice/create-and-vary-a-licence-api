package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
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

  fun getPrisonersByNumber(nomisIds: List<String>) =
    prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds).map { it.toCaseloadItem() }

  fun getPrisonersByReleaseDate(earliestReleaseDate: LocalDate, latestReleaseDate: LocalDate, prisonIds: Set<String>) =
    prisonerSearchApiClient.searchPrisonersByReleaseDate(earliestReleaseDate, latestReleaseDate, prisonIds).map { it.toCaseloadItem() }

  fun getPrisoner(nomisId: String) =
    getPrisonersByNumber(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)

  private fun PrisonerSearchPrisoner.toCaseloadItem(): CaseloadItem {
    val sentenceDateHolder = this.toSentenceDateHolder()
    return CaseloadItem(
      prisoner = this.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(this),
        hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder),
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder),
      ),
    )
  }
}
