package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection

@Service
class PrisonerSearchService(
  private val licenceRepository: LicenceRepository,
  private val communityApiClient: CommunityApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
) {
  fun searchForOffenderOnStaffCaseload(body: ProbationUserSearchRequest): ProbationSearchResult {
    val teamCaseloadResult = probationSearchApiClient.searchLicenceCaseloadByTeam(
      body.query,
      communityApiClient.getTeamsCodesForUser(body.staffIdentifier),
      body.getSortBy(),
    )

    val resultsWithLicences: List<Pair<CaseloadResult, Licence?>> =
      teamCaseloadResult.map { it to getLicence(it) }

    val prisonerRecords = findPrisonersForRelevantRecords(resultsWithLicences)

    val searchResults = resultsWithLicences.mapNotNull { (result, licence) ->
      when (licence) {
        null -> createNotStartedRecord(result, prisonerRecords[result.identifiers.noms])
        else -> createRecord(result, licence, prisonerRecords[result.identifiers.noms])
      }
    }.filterOutHdc(prisonerRecords)

    val onProbationCount = searchResults.count { it.isOnProbation == true }
    val inPrisonCount = searchResults.count { it.isOnProbation == false }

    return ProbationSearchResult(
      searchResults,
      inPrisonCount,
      onProbationCount,
    )
  }

  fun getIneligibilityReasons(prisonNumber: String): List<String> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    val reasons = eligibilityService.getIneligibilityReasons(prisoners.first())
    val hdcReasonIfPresent = if (prisoners.findBookingsWithHdc().isEmpty()) emptyList() else listOf("Approved for HDC")
    return reasons + hdcReasonIfPresent
  }

  private fun PrisonerSearchPrisoner.getReleaseDate() = confirmedReleaseDate ?: releaseDate

  private fun getLicence(result: CaseloadResult): Licence? {
    val licences =
      licenceRepository.findAllByCrnAndStatusCodeIn(result.identifiers.crn, LicenceStatus.IN_FLIGHT_LICENCES)
    return if (licences.isEmpty()) {
      null
    } else {
      val nonActiveLicenceStatuses = LicenceStatus.IN_FLIGHT_LICENCES - LicenceStatus.ACTIVE
      if (licences.size > 1) licences.find { licence -> licence.statusCode in nonActiveLicenceStatuses } else licences.first()
    }
  }

  private fun findPrisonersForRelevantRecords(record: List<Pair<CaseloadResult, Licence?>>): Map<String, PrisonerSearchPrisoner> {
    val prisonNumbers = record
      .filter { (_, licence) -> licence == null || !licence.statusCode.isOnProbation() }
      .mapNotNull { (result, _) -> result.identifiers.noms }

    if (prisonNumbers.isEmpty()) {
      return emptyMap()
    }

    // we gather further data from prisoner search if there is no licence or a create licence
    val prisoners = this.prisonerSearchApiClient.searchPrisonersByNomisIds(prisonNumbers)

    return prisoners.associateBy { it.prisonerNumber }
  }

  private fun createNotStartedRecord(deliusOffender: CaseloadResult, prisonOffender: PrisonerSearchPrisoner?) = when {
    // no match for prisoner in Delius
    prisonOffender == null -> null

    !eligibilityService.isEligibleForCvl(prisonOffender) -> null

    else -> deliusOffender.toUnstartedRecord(prisonOffender)
  }

  private fun createRecord(
    deliusOffender: CaseloadResult,
    licence: Licence,
    prisonOffender: PrisonerSearchPrisoner?,
  ): FoundProbationRecord? =
    when {
      licence.statusCode.isOnProbation() -> deliusOffender.toStartedRecord(licence)
      prisonOffender != null && eligibilityService.isEligibleForCvl(prisonOffender) -> deliusOffender.toStartedRecord(licence)
      else -> null
    }

  private fun ProbationUserSearchRequest.getSortBy() =
    this.sortBy.map {
      ProbationSearchSortByRequest(
        it.field.probationSearchApiSortType,
        if (it.direction == SearchDirection.ASC) "asc" else "desc",
      )
    }

  private fun List<PrisonerSearchPrisoner>.findBookingsWithHdc(): List<Long> {
    val bookingsWithHdc = this
      .filter { it.homeDetentionCurfewEligibilityDate != null }
      .map { it.bookingId.toLong() }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return hdcStatuses.filter { it.approvalStatus == "APPROVED" }.mapNotNull { it.bookingId }
  }

  private fun List<FoundProbationRecord>.filterOutHdc(prisonerRecords: Map<String, PrisonerSearchPrisoner>): List<FoundProbationRecord> {
    if (prisonerRecords.isEmpty()) {
      return this
    }
    val prisonersForHdcCheck =
      this.filter { it.licenceStatus == NOT_STARTED }.mapNotNull { prisonerRecords[it.nomisId] }
    val bookingIdsWithHdc = prisonersForHdcCheck.findBookingsWithHdc()

    val prisonersWithoutHdc = prisonerRecords.values.filterNot { bookingIdsWithHdc.contains(it.bookingId.toLong()) }
    return this.filter { it.isOnProbation == true || prisonersWithoutHdc.any { prisoner -> prisoner.prisonerNumber == it.nomisId } }
  }

  private fun CaseloadResult.toUnstartedRecord(prisonOffender: PrisonerSearchPrisoner): FoundProbationRecord {
    val sentenceDateHolder = prisonOffender.toSentenceDateHolder()
    return this.transformToUnstartedRecord(
      releaseDate = prisonOffender.getReleaseDate(),
      licenceType = LicenceType.getLicenceType(prisonOffender),
      licenceStatus = NOT_STARTED,
      hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder),
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(sentenceDateHolder),
    )
  }

  private fun CaseloadResult.toStartedRecord(licence: Licence) =
    this.transformToModelFoundProbationRecord(
      licence = licence,
      hardStopDate = releaseDateService.getHardStopDate(licence),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
    )
}
