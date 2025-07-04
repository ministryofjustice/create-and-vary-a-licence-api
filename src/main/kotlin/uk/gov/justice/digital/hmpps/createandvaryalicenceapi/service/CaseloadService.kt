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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

@Service
class CaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
) {

  fun getPrisonersByNumber(nomisIds: List<String>): List<CaseloadItem> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds)
    val licenceStartDates = releaseDateService.getLicenceStartDates(prisoners)
    return prisoners.map { it.toCaseloadItem(licenceStartDates[it.prisonerNumber]) }
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
      it.toCaseloadItem(licenceStartDate)
    }
  }

  fun getPrisoner(nomisId: String) = getPrisonersByNumber(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)

  private fun PrisonerSearchPrisoner.toCaseloadItem(licenceStartDate: LocalDate?): CaseloadItem {
    val sentenceDateHolder = this.toSentenceDateHolder(licenceStartDate)
    return CaseloadItem(
      prisoner = this.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(this),
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

  fun determineLicenceKind(nomisRecord: PrisonerSearchPrisoner): LicenceKind {
    val today = LocalDate.now()
    val prrd = nomisRecord.postRecallReleaseDate
    if (prrd?.isAfter(today) == true && prrd.isAfter(nomisRecord.conditionalReleaseDate)) {
      return LicenceKind.PRRD
    }

    val hardstopDate = releaseDateService.getHardStopDate(nomisRecord.toSentenceDateHolder(null))
    if (hardstopDate != null && hardstopDate <= today) {
      return LicenceKind.HARD_STOP
    }

    return LicenceKind.CRD
  }
}
