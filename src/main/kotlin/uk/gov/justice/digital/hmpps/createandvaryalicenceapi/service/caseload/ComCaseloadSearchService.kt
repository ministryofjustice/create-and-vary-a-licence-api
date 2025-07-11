package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_CONFIRMED_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_CRD_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_HDC_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_PRRD_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient.Companion.CASELOAD_PAGE_SIZE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToModelFoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToUnstartedRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.PRE_RELEASE_STATUSES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType.Companion.getLicenceType
import java.time.Clock
import java.time.LocalDate

@Service
class ComCaseloadSearchService(
  private val licenceRepository: LicenceRepository,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val hdcService: HdcService,
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
  private val clock: Clock,
) {
  fun searchForOffenderOnStaffCaseload(body: ProbationUserSearchRequest): ProbationSearchResult {
    val teamCaseloadResult = deliusApiClient.getTeamManagedOffenders(
      body.staffIdentifier,
      body.query,
      PageRequest.of(
        0,
        CASELOAD_PAGE_SIZE,
        Sort.by(body.sortBy.map { Sort.Order(it.direction, it.field.probationSearchApiSortType) }),
      ),
    )

    val deliusRecordsWithLicences: List<Pair<CaseloadResult, Licence?>> =
      teamCaseloadResult.content.map { it to getLicence(it.crn) }

    val prisonerRecords = findPrisonersForRelevantRecords(deliusRecordsWithLicences)

    val cvlSearchRecords = deliusRecordsWithLicences.map { (caseloadResult, licence) ->
      val prisonRecord = prisonerRecords[caseloadResult.nomisId]
      CvlProbationSearchRecord(
        caseloadResult = caseloadResult,
        prisonerSearchPrisoner = prisonRecord,
        licence = licence,
      )
    }

    val prisonersWithoutLicences = cvlSearchRecords.filter { searchRecord -> searchRecord.licence == null }.mapNotNull {
      it.prisonerSearchPrisoner
    }
    val licenceStartDates = releaseDateService.getLicenceStartDates(prisonersWithoutLicences)

    val searchResults = cvlSearchRecords.mapNotNull {
      val licenceStartDate = licenceStartDates[it.prisonerSearchPrisoner?.prisonerNumber]
      when (it.licence) {
        null -> createNotStartedRecord(it.caseloadResult, it.prisonerSearchPrisoner, licenceStartDate)
        else -> createRecord(it.caseloadResult, it.licence, it.prisonerSearchPrisoner)
      }
    }.filterOutPastReleaseDate().filterOutHdc(prisonerRecords)

    val onProbationCount = searchResults.count { it.isOnProbation == true }
    val inPrisonCount = searchResults.count { it.isOnProbation == false }

    return ProbationSearchResult(searchResults, inPrisonCount, onProbationCount)
  }

  private fun getLicence(crn: String): Licence? {
    val licences =
      licenceRepository.findAllByCrnAndStatusCodeIn(crn, IN_FLIGHT_LICENCES)
    return licences.maxWithOrNull(versionComparator)
  }

  private fun findPrisonersForRelevantRecords(record: List<Pair<CaseloadResult, Licence?>>): Map<String, PrisonerSearchPrisoner> {
    val prisonNumbers = record
      .filter { (_, licence) -> licence == null || !licence.statusCode.isOnProbation() }
      .mapNotNull { (result, _) -> result.nomisId }
      .ifEmpty { return emptyMap() }

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
      releaseDateLabel = getReleaseDateLable(licenceStartDate, prisonOffender),
    )
  }

  private fun getReleaseDateLable(
    licenceStartDate: LocalDate?,
    prisonOffender: PrisonerSearchPrisoner,
  ) = when (licenceStartDate) {
    prisonOffender.confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
    prisonOffender.postRecallReleaseDate -> LABEL_FOR_PRRD_RELEASE_DATE
    prisonOffender.homeDetentionCurfewActualDate -> LABEL_FOR_HDC_RELEASE_DATE
    else -> LABEL_FOR_CRD_RELEASE_DATE
  }

  private fun CaseloadResult.toStartedRecord(licence: Licence) = this.transformToModelFoundProbationRecord(
    licence = licence,
    hardStopDate = releaseDateService.getHardStopDate(licence),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence),
    isDueForEarlyRelease = releaseDateService.isDueForEarlyRelease(licence),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence),
  )

  private fun List<FoundProbationRecord>.filterOutPastReleaseDate(): List<FoundProbationRecord> = this.filter {
    if (it.isOnProbation == true) {
      true
    } else {
      it.releaseDate?.isAfter(LocalDate.now(clock).minusDays(1)) == true
    }
  }

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
