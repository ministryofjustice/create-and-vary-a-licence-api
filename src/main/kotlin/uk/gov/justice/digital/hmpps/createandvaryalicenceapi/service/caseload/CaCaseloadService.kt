package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCaseCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCaseDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.View.PRISON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.View.PROBATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.Clock
import java.time.LocalDate

private enum class View(val caseComparator: Comparator<CaCase>) {
  PRISON(
    compareBy<CaCase> { it.releaseDate }
      .thenBy { it.licenceId },
  ),
  PROBATION(
    compareByDescending<CaCase> { it.releaseDate }
      .thenBy { it.licenceId },
  ),
}

@Service
class CaCaseloadService(
  private val caseloadService: CaseloadService,
  private val licenceService: LicenceService,
  private val hdcService: HdcService,
  private val clock: Clock,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
) {
  fun getPrisonOmuCaseload(prisonCaseload: Set<String>, searchString: String?): List<CaCase> {
    val statuses = listOf(
      APPROVED,
      SUBMITTED,
      IN_PROGRESS,
      TIMED_OUT,
      ACTIVE,
    )
    val filteredPrisons = prisonCaseload.filterNot { it == "CADM" }
    val existingLicences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        statusCodes = statuses,
        prisonCodes = filteredPrisons,
        sortBy = "licenceStartDate",
      ),
    )

    val eligibleExistingCases = filterAndFormatExistingCases(existingLicences)

    val eligibleNotStartedCases = findAndFormatNotStartedCases(
      existingLicences,
      prisonCaseload,
    )

    if (eligibleExistingCases.isEmpty() && eligibleNotStartedCases.isEmpty()) {
      return emptyList()
    }

    val cases = mapCasesToComs(eligibleExistingCases + eligibleNotStartedCases)

    return buildCaseload(cases, searchString, PRISON)
  }

  fun getProbationOmuCaseload(
    prisonCaseload: Set<String>,
    searchString: String?,
  ): List<CaCase> {
    val statuses = listOf(
      ACTIVE,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
    )
    val licences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        statusCodes = statuses,
        prisonCodes = prisonCaseload.toList(),
      ),
    )

    if (licences.isEmpty()) {
      return emptyList()
    }

    val formattedLicences = formatReleasedLicences(licences)
    val cases = mapCasesToComs(formattedLicences)

    return buildCaseload(cases, searchString, PROBATION)
  }

  fun searchForOffenderOnPrisonCaseAdminCaseload(body: PrisonUserSearchRequest): PrisonCaseAdminSearchResult {
    val prisonCases = getPrisonOmuCaseload(body.prisonCaseloads, body.query)

    val (attentionNeededCases, inPrisonCases) = prisonCases.partition { it.tabType == CaViewCasesTab.ATTENTION_NEEDED }

    val probationCases = getProbationOmuCaseload(body.prisonCaseloads, body.query)

    return PrisonCaseAdminSearchResult(inPrisonCases, probationCases, attentionNeededCases)
  }

  fun splitCasesByComDetails(cases: List<CaCase>): GroupedByCom = cases.fold(
    GroupedByCom(
      withStaffCode = emptyList(),
      withStaffUsername = emptyList(),
      withNoComId = emptyList(),
    ),
  ) { acc, caCase ->
    if (caCase.probationPractitioner?.staffCode != null) {
      acc.withStaffCode += caCase
    } else if (caCase.probationPractitioner?.staffUsername != null) {
      acc.withStaffUsername += caCase
    } else {
      acc.withNoComId += caCase
    }
    return@fold acc
  }

  fun findLatestLicenceSummary(licences: List<LicenceSummary>): LicenceSummary? {
    if (licences.size == 1) {
      return licences[0]
    }
    if (licences.any { it.licenceStatus == TIMED_OUT }) {
      return licences.find { it.licenceStatus != TIMED_OUT }
    }

    return licences.find { (it.licenceStatus == SUBMITTED) || (it.licenceStatus == IN_PROGRESS) }
  }

  private fun formatReleasedLicences(licences: List<LicenceSummary>): List<CaCase> {
    val groupedLicences = licences.groupBy { it.nomisId }
    return groupedLicences.map {
      val licence = if (it.value.size > 1) {
        it.value.find { l -> l.licenceStatus != ACTIVE }
      } else {
        it.value[0]
      }

      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOf,
        name = "${licence?.forename} ${licence?.surname}",
        prisonerNumber = licence?.nomisId!!,
        releaseDate = licence.licenceStartDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceSummary(licence),
        licenceStatus = licence.licenceStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        probationPractitioner = ProbationPractitioner(
          staffUsername = licence.comUsername,
        ),
        prisonCode = licence.prisonCode,
        prisonDescription = licence.prisonDescription,
      )
    }
  }

  private fun mapCasesToComs(casesToMap: List<CaCase>): List<CaCase> {
    val cases = splitCasesByComDetails(casesToMap)

    val noComPrisonerNumbers = cases.withNoComId.map { c -> c.prisonerNumber }
    val coms = deliusApiClient.getOffenderManagers(noComPrisonerNumbers).associateBy { it.case.nomisId }

    // if no code or username, hit delius to find COM details
    val caCaseListWithNoComId = cases.withNoComId.map { caCase ->
      val com = coms[caCase.prisonerNumber]
      if (com != null && !com.unallocated) {
        caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.code,
            name = com.name.fullName(),
          ),
        )
      } else {
        caCase
      }
    }

    // If COM username but no code, do a separate call to use the data in CVL if it exists. Should help highlight any desync between Delius and CVL
    val comUsernames = cases.withStaffUsername.map { c -> c.probationPractitioner?.staffUsername!! }
    val deliusStaffNames =
      deliusApiClient.getStaffDetailsByUsername(comUsernames).associateBy { it.username?.lowercase() }

    val caCaseListWithStaffUsername = cases.withStaffUsername.map { caCase ->
      val com = deliusStaffNames[caCase.probationPractitioner?.staffUsername?.lowercase()]
      if (com != null) {
        caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.code,
            name = com.name.fullName(),
          ),
        )
      } else {
        caCase
      }
    }

    return caCaseListWithNoComId + caCaseListWithStaffUsername + cases.withStaffCode
  }

  private fun buildCaseload(cases: List<CaCase>, searchString: String?, view: View): List<CaCase> {
    val searchResults = applySearch(searchString, cases)
    return searchResults.sortedWith(view.caseComparator)
  }

  private fun applySearch(searchString: String?, cases: List<CaCase>): List<CaCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter {
      it.name.lowercase().contains(term) ||
        it.prisonerNumber.lowercase().contains(term) ||
        it.probationPractitioner?.name?.lowercase()?.contains(term) ?: false
    }
  }

  private fun filterAndFormatExistingCases(licences: List<LicenceSummary>): List<CaCase> {
    val preReleaseLicences = licences.filter { it.licenceStatus != ACTIVE }
    if (preReleaseLicences.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = preReleaseLicences.map { it.nomisId }
    val prisonersWithLicences = caseloadService.getPrisonersByNumber(licenceNomisIds)
    val nomisEnrichedLicences = enrichWithNomisData(preReleaseLicences, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun findAndFormatNotStartedCases(
    licences: List<LicenceSummary>,
    prisonCaseload: Set<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { it.nomisId }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisonerNumber)
    }.toList()

    val nomisRecordsToDeliusRecords = pairNomisRecordsWithDelius(prisonersWithoutLicences)
    val nomisIdsToAreaCodes = nomisRecordsToDeliusRecords.associate { (nomisRecord, deliusRecord) -> nomisRecord.prisonerNumber to deliusRecord.team.provider.code }

    val cvlRecords = cvlRecordService.getCvlRecords(prisonersWithoutLicences, nomisIdsToAreaCodes)

    val casesWithoutLicences = buildManagedCaseDto(nomisRecordsToDeliusRecords, cvlRecords)

    val eligibleCases = filterOffendersEligibleForLicence(casesWithoutLicences)

    return createNotStartedLicenceForCase(eligibleCases)
  }

  private fun createNotStartedLicenceForCase(
    cases: List<ManagedCaseDto>,
  ): List<CaCase> = cases.map { case ->
    val sentenceDateHolder = case.nomisRecord.toSentenceDateHolder(case.cvlRecord.licenceStartDate)

    // Default status (if not overridden below) will show the case as clickable on case lists
    var licenceStatus = NOT_STARTED

    if (releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate)) {
      licenceStatus = TIMED_OUT
    }

    val com = case.deliusRecord.managedOffenderCrn.staff

    CaCase(
      kind = case.cvlRecord.eligibleKind,
      name = case.nomisRecord.let { "${it.firstName} ${it.lastName}".convertToTitleCase() },
      prisonerNumber = case.nomisRecord.prisonerNumber,
      releaseDate = case.cvlRecord.licenceStartDate,
      releaseDateLabel = releaseDateLabelFactory.fromPrisonerSearch(case.cvlRecord.licenceStartDate, case.nomisRecord),
      licenceStatus = licenceStatus,
      nomisLegalStatus = case.nomisRecord.legalStatus,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate),
      tabType = Tabs.determineCaViewCasesTab(
        releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        case.cvlRecord.licenceStartDate,
        licence = null,
        clock,
      ),
      probationPractitioner = ProbationPractitioner(
        staffCode = com?.code,
        name = com?.name?.fullName(),
      ),
      prisonCode = case.nomisRecord.prisonId,
      prisonDescription = case.nomisRecord.prisonName,
    )
  }

  private fun filterOffendersEligibleForLicence(offenders: List<ManagedCaseDto>): List<ManagedCaseDto> {
    val eligibleOffenders = offenders.filter { it.cvlRecord.isEligible }

    if (eligibleOffenders.isEmpty()) return eligibleOffenders

    val hdcStatuses = hdcService.getHdcStatus(eligibleOffenders.map { it.nomisRecord })

    return eligibleOffenders.filter { hdcStatuses.canUnstartedCaseBeSeenByCa(it.nomisRecord.bookingId?.toLong()!!) }
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> = licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun enrichWithNomisData(licences: List<LicenceSummary>, nomisRecords: List<PrisonerSearchPrisoner>): List<CaCase> {
    return nomisRecords.map { nomisRecord ->
      val licencesForOffender = licences.filter { l -> l.nomisId == nomisRecord.prisonerNumber }
      if (licencesForOffender.isEmpty()) return@map null
      val licence = findLatestLicenceSummary(licencesForOffender)
      val releaseDate = licence?.licenceStartDate
      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOf,
        name = licence.let { "${it?.forename} ${it?.surname}" },
        prisonerNumber = licence?.nomisId!!,
        releaseDate = releaseDate,
        releaseDateLabel = releaseDateLabelFactory.fromLicenceSummary(licence),
        licenceStatus = licence.licenceStatus,
        nomisLegalStatus = nomisRecord.legalStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(licence.toSentenceDateHolder()),
          releaseDate,
          licence,
          clock,
        ),
        probationPractitioner = ProbationPractitioner(staffUsername = licence.comUsername),
        prisonCode = licence.prisonCode,
        prisonDescription = licence.prisonDescription,
      )
    }.filterNotNull()
  }

  private fun getPrisonersApproachingRelease(
    prisonCaseload: Set<String>,
    overrideClock: Clock? = null,
  ): Page<PrisonerSearchPrisoner> {
    val now = overrideClock ?: clock
    val weeksToAdd: Long = 4
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(weeksToAdd)
    return prisonCaseload.let {
      prisonerSearchApiClient.searchPrisonersByReleaseDate(
        today,
        todayPlusFourWeeks,
        it,
        page = 0,
      )
    }
  }

  private fun pairNomisRecordsWithDelius(nomisRecords: List<PrisonerSearchPrisoner>): List<Pair<PrisonerSearchPrisoner, CommunityManager>> {
    val caseloadNomisIds = nomisRecords.map { it.prisonerNumber }
    val coms = deliusApiClient.getOffenderManagers(caseloadNomisIds)
    return nomisRecords.mapNotNull {
      val com = coms.find { com -> com.case.nomisId == it.prisonerNumber }
      if (com != null) {
        it to com
      } else {
        null
      }
    }
  }

  private fun buildManagedCaseDto(
    nomisRecordsToComRecords: List<Pair<PrisonerSearchPrisoner, CommunityManager>>,
    cvlRecords: List<CvlRecord>,
  ): List<ManagedCaseDto> = nomisRecordsToComRecords
    .map { (nomisRecord, com) ->
      val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
      ManagedCaseDto(
        nomisRecord = nomisRecord,
        cvlRecord = ManagedCaseCvlRecord(
          isEligible = cvlRecord.isEligible,
          eligibleKind = cvlRecord.eligibleKind,
          licenceStartDate = cvlRecord.licenceStartDate,
        ),
        deliusRecord = DeliusRecord(
          com.case,
          ManagedOffenderCrn(
            staff = StaffDetail(
              code = com.code,
              name = com.name,
              unallocated = com.unallocated,
            ),
            team = com.team,
          ),
        ),
      )
    }

  data class GroupedByCom(
    var withStaffCode: List<CaCase>,
    var withStaffUsername: List<CaCase>,
    var withNoComId: List<CaCase>,
  )
}
