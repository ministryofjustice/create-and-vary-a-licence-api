package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.com

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ProbationUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.ComSearchResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.FoundComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.getVersionOf
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CaseloadResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient.Companion.CASELOAD_PAGE_SIZE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.CaseAccessResponse.Companion.unrestricted
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToUnstartedRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReviewablePostRelease
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.Clock
import java.time.LocalDate

@Service
class ComCaseloadSearchService(
  private val licenceRepository: LicenceRepository,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
  private val clock: Clock,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
  @param:Value("\${feature.toggle.lao.enabled}") private val laoEnabled: Boolean = false,
) {
  @Transactional()
  fun searchForOffenderOnProbationUserCaseload(body: ProbationUserSearchRequest): ComSearchResponse {
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
    val cvlRecordsByPrisonNumber =
      cvlRecordService.getCvlRecords(prisonerRecords.values.toList()).associateBy { it.nomisId }

    val caseAccessRecords = if (laoEnabled) {
      getCaseAccessRecords(deliusRecordsToLicences)
    } else {
      emptyMap()
    }

    val searchResults = deliusRecordsToLicences.mapNotNull { (caseloadResult, licence) ->
      val prisonerRecord = prisonerRecords[caseloadResult.nomisId]
      val cvlRecord = cvlRecordsByPrisonNumber[caseloadResult.nomisId]
      val caseAccessRecord = caseAccessRecords[caseloadResult.crn] ?: unrestricted
      val releaseDate = determineReleaseDate(cvlRecord, licence)
      val case = createCase(licence, caseloadResult, prisonerRecord, cvlRecord, caseAccessRecord)
      shouldExcludeCase(body.query, cvlRecord, caseAccessRecord, licence, case, releaseDate)
    }

    val onProbationCount = searchResults.count { it.isOnProbation == true }
    val inPrisonCount = searchResults.count { it.isOnProbation == false }

    return ComSearchResponse(
      results = searchResults,
      inPrisonCount = inPrisonCount,
      onProbationCount = onProbationCount,
    )
  }

  private fun createCase(
    licence: Licence?,
    caseloadResult: CaseloadResult,
    prisonerRecord: PrisonerSearchPrisoner?,
    cvlRecord: CvlRecord?,
    caseAccessRecord: CaseAccessResponse,
  ): FoundComCase? = if (licence == null) {
    createNotStartedCase(caseloadResult, prisonerRecord, cvlRecord, caseAccessRecord)
  } else {
    createCaseWithExistingLicence(caseloadResult, licence, prisonerRecord, cvlRecord, caseAccessRecord)
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

  private fun createNotStartedCase(
    deliusOffender: CaseloadResult,
    prisonOffender: PrisonerSearchPrisoner?,
    cvlRecord: CvlRecord?,
    caseAccessRecord: CaseAccessResponse,
  ) = when {
    // no match for prisoner in Delius
    prisonOffender == null || cvlRecord == null -> null

    !cvlRecord.isEligible -> null

    else -> deliusOffender.toUnstartedRecord(prisonOffender, cvlRecord, caseAccessRecord)
  }

  private fun createCaseWithExistingLicence(
    deliusOffender: CaseloadResult,
    licence: Licence,
    prisonOffender: PrisonerSearchPrisoner?,
    cvlRecord: CvlRecord?,
    caseAccessRecord: CaseAccessResponse,
  ): FoundComCase? = when {
    licence.statusCode.isOnProbation() -> deliusOffender.toCaseWithLicence(licence, caseAccessRecord)
    prisonOffender == null || cvlRecord == null -> null
    cvlRecord.isEligible -> deliusOffender.toCaseWithLicence(licence, caseAccessRecord)
    else -> null
  }

  private fun CaseloadResult.toUnstartedRecord(
    prisonOffender: PrisonerSearchPrisoner,
    cvlRecord: CvlRecord,
    caseAccessRecord: CaseAccessResponse,
  ): FoundComCase = this.transformToUnstartedRecord(
    cvlRecord.hardStopKind ?: cvlRecord.eligibleKind!!,
    releaseDate = cvlRecord.licenceStartDate,
    bookingId = prisonOffender.bookingId?.toLong(),
    licenceType = cvlRecord.licenceType,
    licenceStatus = if (cvlRecord.isTimedOut) TIMED_OUT else NOT_STARTED,
    hardStopDate = cvlRecord.hardStopDate,
    hardStopWarningDate = cvlRecord.hardStopWarningDate,
    isInHardStopPeriod = cvlRecord.isInHardStopPeriod,
    isDueToBeReleasedInTheNextTwoWorkingDays = cvlRecord.isDueToBeReleasedInTheNextTwoWorkingDays,
    releaseDateLabel = releaseDateLabelFactory.fromPrisonerSearch(cvlRecord.licenceStartDate, prisonOffender),
    isExcluded = caseAccessRecord.userExcluded,
    isRestricted = caseAccessRecord.userRestricted,
  )

  private fun CaseloadResult.toCaseWithLicence(
    licence: Licence,
    caseAccessRecord: CaseAccessResponse,
  ) = this.transformToCaseWithLicence(
    licence = licence,
    hardStopDate = releaseDateService.getHardStopDate(licence.licenceStartDate, licence.kind),
    hardStopWarningDate = releaseDateService.getHardStopWarningDate(licence.licenceStartDate, licence.kind),
    isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licence.licenceStartDate, licence.kind),
    isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.licenceStartDate),
    isExcluded = caseAccessRecord.userExcluded,
    isRestricted = caseAccessRecord.userRestricted,
  )

  private fun getProbationPractitioner(staff: StaffDetail): ProbationPractitioner = if (staff.unallocated == true) {
    ProbationPractitioner.unallocated(staff.code)
  } else {
    ProbationPractitioner(
      staffCode = staff.code,
      name = staff.name!!.fullName(),
      allocated = true,
    )
  }

  fun CaseloadResult.transformToCaseWithLicence(
    licence: Licence,
    hardStopDate: LocalDate?,
    hardStopWarningDate: LocalDate?,
    isInHardStopPeriod: Boolean,
    isDueToBeReleasedInTheNextTwoWorkingDays: Boolean,
    isExcluded: Boolean,
    isRestricted: Boolean,
  ): FoundComCase {
    if (isExcluded || isRestricted) {
      return FoundComCase.restrictedCase(licence.kind, crn, licence.statusCode.isOnProbation())
    }

    val com = if (staff.unallocated == true) null else staff
    val probationPractitioner = getProbationPractitioner(staff)

    return FoundComCase(
      kind = licence.kind,
      bookingId = licence.bookingId,
      name = "${name.forename} ${name.surname}".convertToTitleCase(),
      crn = licence.crn,
      nomisId = licence.nomsId,
      comName = com?.name?.fullName()?.convertToTitleCase(),
      comStaffCode = com?.code,
      probationPractitioner = probationPractitioner,
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
      isLao = false,
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

  private fun getCaseAccessRecords(deliusRecordsToLicences: List<Pair<CaseloadResult, Licence?>>): Map<String, CaseAccessResponse> {
    val username = SecurityContextHolder.getContext().authentication.name
    val crns = deliusRecordsToLicences.map { (caseloadResult, _) -> caseloadResult.crn }
    return deliusApiClient.getCheckUserAccess(username, crns).associateBy { it.crn }
  }

  private fun determineReleaseDate(cvlRecord: CvlRecord?, licence: Licence?): LocalDate? = when {
    licence != null -> licence.licenceStartDate
    cvlRecord?.isEligible == true -> cvlRecord.licenceStartDate
    else -> null
  }

  private fun isExcludedFromComCreateVaryCaseloads(releaseDate: LocalDate?, licence: Licence?, cvlRecord: CvlRecord?): Boolean = when {
    licence?.statusCode?.isOnProbation() == true -> false
    releaseDate?.isAfter(LocalDate.now(clock).minusDays(1)) == true -> false
    cvlRecord?.hardStopKind == TIME_SERVED -> false
    licence?.kind == TIME_SERVED -> false
    else -> true
  }

  private fun CaseAccessResponse.isExcludedFromViewingRestrictedCase(searchTerm: String): Boolean = !searchTerm.matches(CRN_REGEX) && (userExcluded || userRestricted)

  private fun shouldExcludeCase(
    searchTerm: String,
    cvlRecord: CvlRecord?,
    caseAccessRecord: CaseAccessResponse,
    licence: Licence?,
    case: FoundComCase?,
    releaseDate: LocalDate?,
  ): FoundComCase? = when {
    // Exclude restricted or excluded cases where the search term is not the CRN
    caseAccessRecord.isExcludedFromViewingRestrictedCase(searchTerm) -> null

    // Exclude based on other rules
    isExcludedFromComCreateVaryCaseloads(releaseDate, licence, cvlRecord) -> null

    // Include the case if no exclusions above apply
    else -> case
  }

  companion object {
    val CRN_REGEX = "^[A-Za-z]\\d{6}$".toRegex()
  }
}
