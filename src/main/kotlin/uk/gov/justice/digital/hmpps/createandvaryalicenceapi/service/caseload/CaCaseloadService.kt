package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonCaseAdminSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.PrisonUserSearchRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_CONFIRMED_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_CRD_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_HDC_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LABEL_FOR_PRRD_RELEASE_DATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.toPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.Clock
import java.time.LocalDate

@Service
class CaCaseloadService(
  private val caseloadService: CaseloadService,
  private val licenceService: LicenceService,
  private val hdcService: HdcService,
  private val eligibilityService: EligibilityService,
  private val clock: Clock,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
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

    val eligibleExistingLicences = filterAndFormatExistingLicences(existingLicences)

    val eligibleNotStartedLicences = findAndFormatNotStartedLicences(
      existingLicences,
      prisonCaseload,
    )

    if (eligibleExistingLicences.isEmpty() && eligibleNotStartedLicences.isEmpty()) {
      return emptyList()
    }

    val cases = mapCasesToComs(eligibleExistingLicences + eligibleNotStartedLicences)

    return buildCaseload(cases, searchString, "prison")
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

    return buildCaseload(cases, searchString, "probation")
  }

  fun searchForOffenderOnPrisonCaseAdminCaseload(body: PrisonUserSearchRequest): PrisonCaseAdminSearchResult {
    val inPrisonResults = getPrisonOmuCaseload(body.prisonCaseloads, body.query)
    val onProbationResults = getProbationOmuCaseload(body.prisonCaseloads, body.query)

    return PrisonCaseAdminSearchResult(inPrisonResults, onProbationResults)
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
        releaseDateLabel = getReleaseDateLabel(licence),
        licenceStatus = licence.licenceStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isDueForEarlyRelease = licence.isDueForEarlyRelease,
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
            name = com.name?.fullName(),
          ),
        )
      } else {
        caCase
      }
    }

    return caCaseListWithNoComId + caCaseListWithStaffUsername + cases.withStaffCode
  }

  private fun buildCaseload(cases: List<CaCase>, searchString: String?, view: String): List<CaCase> {
    val searchResults = applySearch(searchString, cases)
    val sortResults = applySort(searchResults, view)
    return sortResults
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

  private fun applySort(cases: List<CaCase>, view: String): List<CaCase> = when (view) {
    "prison" -> cases.sortedWith(
      compareBy<CaCase> { it.releaseDate }
        .thenBy { it.licenceId },
    )

    "probation" -> cases.sortedWith(
      compareByDescending<CaCase> { it.releaseDate }
        .thenBy { it.licenceId },
    )

    else -> cases
  }

  private fun filterAndFormatExistingLicences(licences: List<LicenceSummary>): List<CaCase> {
    val preReleaseLicences = licences.filter { it.licenceStatus != ACTIVE }
    if (preReleaseLicences.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = preReleaseLicences.map { it.nomisId }
    val prisonersWithLicences = caseloadService.getPrisonersByNumber(licenceNomisIds)
    val nomisEnrichedLicences = this.enrichWithNomisData(preReleaseLicences, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun findAndFormatNotStartedLicences(
    licences: List<LicenceSummary>,
    prisonCaseload: Set<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { it.nomisId }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisonerNumber)
    }

    val eligiblePrisoners = filterOffendersEligibleForLicence(prisonersWithoutLicences.toList())

    val licenceStartDates = releaseDateService.getLicenceStartDates(eligiblePrisoners)

    val casesWithoutLicences = pairNomisRecordsWithDelius(eligiblePrisoners, licenceStartDates)
    return createNotStartedLicenceForCase(casesWithoutLicences, licenceStartDates)
  }

  private fun createNotStartedLicenceForCase(
    cases: List<ManagedCase>,
    licenceStartDates: Map<String, LocalDate?>,
  ): List<CaCase> = cases.map { case ->

    // Default status (if not overridden below) will show the case as clickable on case lists
    var licenceStatus = NOT_STARTED

    if (case.cvlFields.isInHardStopPeriod) {
      licenceStatus = TIMED_OUT
    }

    val com = case.deliusRecord?.managedOffenderCrn?.staff

    val releaseDate = licenceStartDates[case.nomisRecord?.prisonerNumber]

    CaCase(
      name = case.nomisRecord.let { "${it?.firstName} ${it?.lastName}".convertToTitleCase() },
      prisonerNumber = case.nomisRecord?.prisonerNumber!!,
      releaseDate = releaseDate,
      releaseDateLabel = getReleaseDateLabel(releaseDate, case.nomisRecord),
      licenceStatus = licenceStatus,
      nomisLegalStatus = case.nomisRecord.legalStatus,
      isDueForEarlyRelease = case.cvlFields.isDueForEarlyRelease,
      isInHardStopPeriod = case.cvlFields.isInHardStopPeriod,
      tabType = Tabs.determineCaViewCasesTab(case.cvlFields, releaseDate, licence = null, clock),
      probationPractitioner = ProbationPractitioner(
        staffCode = com?.code,
        name = com?.name?.fullName(),
      ),
      prisonCode = case.nomisRecord.prisonId,
      prisonDescription = case.nomisRecord.prisonName,
    )
  }

  private fun filterOffendersEligibleForLicence(offenders: List<PrisonerSearchPrisoner>): List<PrisonerSearchPrisoner> {
    val eligibleOffenders = offenders.filter { eligibilityService.isEligibleForCvl(it) }

    if (eligibleOffenders.isEmpty()) return eligibleOffenders

    val hdcStatuses = hdcService.getHdcStatus(eligibleOffenders)

    return eligibleOffenders.filter { hdcStatuses.canUnstartedCaseBeSeenByCa(it.bookingId?.toLong()!!) }
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> = licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun enrichWithNomisData(licences: List<LicenceSummary>, caseloadItems: List<CaseloadItem>): List<CaCase> {
    return caseloadItems.map { caseloadItem ->
      val licencesForOffender = licences.filter { l -> l.nomisId == caseloadItem.prisoner.prisonerNumber }
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
        releaseDateLabel = getReleaseDateLabel(licence),
        licenceStatus = licence.licenceStatus,
        nomisLegalStatus = caseloadItem.prisoner.legalStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isDueForEarlyRelease = licence.isDueForEarlyRelease,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          caseloadItem.cvl,
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

  private fun getReleaseDateLabel(
    licence: LicenceSummary,
  ): String = when (licence.licenceStartDate) {
    null -> LABEL_FOR_CRD_RELEASE_DATE
    licence.actualReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
    licence.postRecallReleaseDate -> LABEL_FOR_PRRD_RELEASE_DATE
    licence.homeDetentionCurfewActualDate -> LABEL_FOR_HDC_RELEASE_DATE
    else -> LABEL_FOR_CRD_RELEASE_DATE
  }

  private fun getReleaseDateLabel(
    releaseDate: LocalDate?,
    nomisRecord: Prisoner,
  ): String = when (releaseDate) {
    null -> LABEL_FOR_CRD_RELEASE_DATE
    nomisRecord.confirmedReleaseDate -> LABEL_FOR_CONFIRMED_RELEASE_DATE
    nomisRecord.postRecallReleaseDate -> LABEL_FOR_PRRD_RELEASE_DATE
    nomisRecord.homeDetentionCurfewActualDate -> LABEL_FOR_HDC_RELEASE_DATE
    else -> LABEL_FOR_CRD_RELEASE_DATE
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

  private fun pairNomisRecordsWithDelius(
    prisoners: List<PrisonerSearchPrisoner>,
    licenceStartDates: Map<String, LocalDate?>,
  ): List<ManagedCase> {
    val caseloadNomisIds = prisoners
      .map { it.prisonerNumber }

    val coms = deliusApiClient.getOffenderManagers(caseloadNomisIds)

    return prisoners
      .mapNotNull { prisoner ->
        val licenceStartDate = licenceStartDates[prisoner.prisonerNumber]
        val com = coms.find { com -> com.case.nomisId == prisoner.prisonerNumber }
        if (com != null) {
          ManagedCase(
            nomisRecord = prisoner.toPrisoner(),
            cvlFields = prisoner.toCaseloadItem(licenceStartDate).cvl,
            deliusRecord = DeliusRecord(
              com.case,
              ManagedOffenderCrn(
                staff = StaffDetail(
                  code = com.code,
                  name = com.name,
                  unallocated = com.unallocated,
                ),
              ),
            ),
          )
        } else {
          null
        }
      }
  }

  data class GroupedByCom(
    var withStaffCode: List<CaCase>,
    var withStaffUsername: List<CaCase>,
    var withNoComId: List<CaCase>,
  )
}
