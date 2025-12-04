package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.FoundProbationRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.getVersionOf
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient.Companion.CASELOAD_PAGE_SIZE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToUnstartedRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReviewablePostRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.PRE_RELEASE_STATUSES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.Clock
import java.time.LocalDate

@Service
class ComCaseloadSearchService(
  private val licenceRepository: LicenceRepository,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val hdcService: HdcService,
  private val releaseDateService: ReleaseDateService,
  private val clock: Clock,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
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
    ).content

    val deliusRecordsToLicences = teamCaseloadResult.map { it to getLicence(it.crn) }
    val prisonerRecords = findPrisonersForRelevantRecords(deliusRecordsToLicences)
    val cvlRecords = cvlRecordService.getCvlRecords(prisonerRecords.values.toList())

    val cvlSearchRecords = deliusRecordsToLicences.map { (caseloadResult, licence) ->
      val prisonRecord = prisonerRecords[caseloadResult.nomisId]
      CvlProbationSearchRecord(
        caseloadResult = caseloadResult,
        prisonerSearchPrisoner = prisonRecord,
        licence = licence,
      )
    }

    val searchResults = cvlSearchRecords.mapNotNull {
      val cvlRecord: CvlRecord? =
        cvlRecords.find { cvlRecord -> it.prisonerSearchPrisoner?.prisonerNumber == cvlRecord.nomisId }
      when (it.licence) {
        null -> createNotStartedRecord(it.caseloadResult, it.prisonerSearchPrisoner, cvlRecord)
        else -> createRecord(it.caseloadResult, it.licence, it.prisonerSearchPrisoner, cvlRecord)
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
    cvlRecord: CvlRecord?,
  ) = when {
    // no match for prisoner in Delius
    prisonOffender == null || cvlRecord == null -> null

    !cvlRecord.isEligible -> null
    else -> deliusOffender.toUnstartedRecord(prisonOffender, cvlRecord)
  }

  private fun createRecord(
    deliusOffender: CaseloadResult,
    licence: Licence,
    prisonOffender: PrisonerSearchPrisoner?,
    cvlRecord: CvlRecord?,
  ): FoundProbationRecord? = when {
    licence.statusCode.isOnProbation() -> deliusOffender.toStartedRecord(licence)

    prisonOffender == null || cvlRecord == null -> null
    cvlRecord.isEligible -> deliusOffender.toStartedRecord(licence, cvlRecord.hardStopKind)
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
    cvlRecord: CvlRecord,
  ): FoundProbationRecord = this.transformToUnstartedRecord(
    cvlRecord.hardStopKind,
    releaseDate = cvlRecord.licenceStartDate,
    bookingId = prisonOffender.bookingId?.toLong(),
    licenceType = cvlRecord.licenceType,
    licenceStatus = if (cvlRecord.isInHardStopPeriod) TIMED_OUT else NOT_STARTED,
    hardStopDate = cvlRecord.hardStopDate,
    hardStopWarningDate = cvlRecord.hardStopWarningDate,
    isInHardStopPeriod = cvlRecord.isInHardStopPeriod,
    isDueToBeReleasedInTheNextTwoWorkingDays = cvlRecord.isDueToBeReleasedInTheNextTwoWorkingDays,
    releaseDateLabel = releaseDateLabelFactory.fromPrisonerSearch(cvlRecord.licenceStartDate, prisonOffender),
  )

  private fun CaseloadResult.toStartedRecord(
    licence: Licence,
    hardStopKind: LicenceKind? = null,
  ) = this.transformToModelFoundProbationRecord(
    licence = licence,
    hardStopKind = hardStopKind,
    hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
  )

  private fun List<FoundProbationRecord>.filterOutPastReleaseDate(): List<FoundProbationRecord> = this.filter {
    if (it.isOnProbation == true) {
      true
    } else {
      it.releaseDate?.isAfter(LocalDate.now(clock).minusDays(1)) == true
    }
  }

  fun CaseloadResult.transformToModelFoundProbationRecord(
    licence: Licence,
    hardStopKind: LicenceKind? = null,
    hardStopDate: LocalDate?,
    hardStopWarningDate: LocalDate?,
    isInHardStopPeriod: Boolean,
    isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
  ): FoundProbationRecord {
    val com = if (staff.unallocated == true) null else staff

    return FoundProbationRecord(
      kind = licence.kind,
      hardStopKind = hardStopKind,
      bookingId = licence.bookingId,
      name = "${name.forename} ${name.surname}".convertToTitleCase(),
      crn = licence.crn,
      nomisId = licence.nomsId,
      comName = com?.name?.fullName()?.convertToTitleCase(),
      comStaffCode = com?.code,
      teamName = team.description,
      releaseDate = licence.licenceStartDate,
      licenceId = licence.id,
      versionOf = getVersionOf(licence),
      licenceType = licence.typeCode,
      licenceStatus = licence.statusCode,
      isOnProbation = licence.statusCode.isOnProbation(),
      hardStopDate = hardStopDate,
      hardStopWarningDate = hardStopWarningDate,
      isInHardStopPeriod = isInHardStopPeriod,
      isDueToBeReleasedInTheNextTwoWorkingDays = isDueToBeReleasedInTheNextTwoWorkingDays,
      releaseDateLabel = releaseDateLabelFactory.fromLicence(licence),
      isReviewNeeded = licence is ReviewablePostRelease && licence.isReviewNeeded(),
    )
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
