package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.request.ProbationSearchSortByRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToModelFoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToUnstartedRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.PRE_RELEASE_STATUSES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.Companion.getLicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.SearchDirection
import java.time.LocalDate

@Service
class ComCaseloadSearchService(
  private val licenceRepository: LicenceRepository,
  private val deliusApiClient: DeliusApiClient,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val hdcService: HdcService,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
) {
  fun searchForOffenderOnStaffCaseload(body: ProbationUserSearchRequest): ProbationSearchResult {
    val teamCaseloadResult = probationSearchApiClient.searchLicenceCaseloadByTeam(
      body.query,
      deliusApiClient.getTeamsCodesForUser(body.staffIdentifier),
      body.getSortBy(),
    )

    val deliusRecordsWithLicences: List<Pair<CaseloadResult, Licence?>> =
      teamCaseloadResult.map { it to getLicence(it) }

    val prisonerRecords = findPrisonersForRelevantRecords(deliusRecordsWithLicences)

    val cvlSearchRecords = deliusRecordsWithLicences.map { (caseloadResult, licence) ->
      val prisonRecord = prisonerRecords[caseloadResult.identifiers.noms]
      CvlProbationSearchRecord(
        caseloadResult = caseloadResult,
        prisonerSearchPrisoner = prisonRecord,
        licence = licence,
      )
    }

    val prisonersWithoutLicences = cvlSearchRecords.filter { searchRecord -> searchRecord.licence == null }.map {
      it.prisonerSearchPrisoner
    }
    val licenceStartDates = releaseDateService.getLicenceStartDates(prisonersWithoutLicences)

    val searchResults = cvlSearchRecords.mapNotNull {
      val licenceStartDate = licenceStartDates[it.prisonerSearchPrisoner?.prisonerNumber]
      when (it.licence) {
        null -> createNotStartedRecord(it.caseloadResult, it.prisonerSearchPrisoner, licenceStartDate)
        else -> createRecord(it.caseloadResult, it.licence, it.prisonerSearchPrisoner)
      }
    }.filterOutHdc(prisonerRecords)

    val onProbationCount = searchResults.count { it.isOnProbation == true }
    val inPrisonCount = searchResults.count { it.isOnProbation == false }

    return ProbationSearchResult(searchResults, inPrisonCount, onProbationCount)
  }

  private fun getLicence(result: CaseloadResult): Licence? {
    val licences =
      licenceRepository.findAllByCrnAndStatusCodeIn(result.identifiers.crn, IN_FLIGHT_LICENCES)
    return licences.maxWithOrNull(versionComparator)
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

  private fun createNotStartedRecord(
    deliusOffender: CaseloadResult,
    prisonOffender: PrisonerSearchPrisoner?,
    licenceStartDate: LocalDate?,
  ) = when {
    // no match for prisoner in Delius
    prisonOffender == null -> null

    !eligibilityService.isEligibleForCvl(prisonOffender) -> null

    else -> deliusOffender.toUnstartedRecord(prisonOffender, licenceStartDate)
  }

  private fun createRecord(
    deliusOffender: CaseloadResult,
    licence: Licence,
    prisonOffender: PrisonerSearchPrisoner?,
  ): FoundProbationRecord? = when {
    licence.statusCode.isOnProbation() ->
      deliusOffender.toStartedRecord(licence)

    prisonOffender != null && eligibilityService.isEligibleForCvl(prisonOffender) ->
      deliusOffender.toStartedRecord(licence)

    else -> null
  }

  private fun ProbationUserSearchRequest.getSortBy() = this.sortBy.map {
    ProbationSearchSortByRequest(
      it.field.probationSearchApiSortType,
      if (it.direction == SearchDirection.ASC) "asc" else "desc",
    )
  }

  private fun List<FoundProbationRecord>.filterOutHdc(prisoners: Map<String, PrisonerSearchPrisoner>): List<FoundProbationRecord> {
    if (prisoners.isEmpty()) {
      return this
    }
    val prisonersForHdcCheck =
      this.filter { PRE_RELEASE_STATUSES.contains(it.licenceStatus) }.mapNotNull { prisoners[it.nomisId] }

    val hdcStatuses = hdcService.getHdcStatus(prisonersForHdcCheck)
    return this.filter { it.isOnProbation == true || hdcStatuses.canBeSeenByCom(it.kind, it.bookingId!!) }
  }

  private fun CaseloadResult.toUnstartedRecord(
    prisonOffender: PrisonerSearchPrisoner,
    licenceStartDate: LocalDate?,
  ): FoundProbationRecord {
    val sentenceDateHolder = prisonOffender.toSentenceDateHolder(licenceStartDate)
    val inHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder)

    return this.transformToUnstartedRecord(
      releaseDate = licenceStartDate,
      bookingId = prisonOffender.bookingId?.toLong(),
      licenceType = getLicenceType(prisonOffender),
      licenceStatus = if (inHardStopPeriod) TIMED_OUT else NOT_STARTED,
      hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder),
      hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder),
      isInHardStopPeriod = inHardStopPeriod,
      isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(sentenceDateHolder),
      isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
        sentenceDateHolder,
      ),
      releaseDateLabel = when (licenceStartDate) {
        prisonOffender.homeDetentionCurfewActualDate -> "HDCAD"
        prisonOffender.confirmedReleaseDate -> "Confirmed release date"
        else -> "CRD"
      },
    )
  }

  private fun CaseloadResult.toStartedRecord(licence: Licence) = this.transformToModelFoundProbationRecord(
    licence = licence,
    hardStopDate = releaseDateService.getHardStopDate(licence),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
    isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
  )

  private val versionComparator = Comparator<Licence> { l1, l2 ->
    val (major1, minor1) = l1.licenceVersion?.split('.')?.mapNotNull { it.toIntOrNull() } ?: listOf(0, 0)
    val (major2, minor2) = l2.licenceVersion?.split('.')?.mapNotNull { it.toIntOrNull() } ?: listOf(0, 0)
    val majorComparison = major1.compareTo(major2)
    if (majorComparison != 0) {
      majorComparison
    } else {
      minor1.compareTo(minor2)
    }
  }

  private data class CvlProbationSearchRecord(
    val caseloadResult: CaseloadResult,
    val prisonerSearchPrisoner: PrisonerSearchPrisoner?,
    val licence: Licence? = null,
  )
}
