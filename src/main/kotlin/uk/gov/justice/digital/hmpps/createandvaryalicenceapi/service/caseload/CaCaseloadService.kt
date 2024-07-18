package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCaseLoad
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaseloadItem
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.GroupedByCom
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.getLicenceStartDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.OOS_BOTUS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.OOS_RECALL
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isBreachOfTopUpSupervision
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isRecall
import java.time.Clock
import java.time.LocalDate

@Service
class CaCaseloadService(
  private val caseloadService: CaseloadService,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
  private val eligibilityService: EligibilityService,
  private val clock: Clock,
  private val communityApiClient: CommunityApiClient,
) {
  fun getPrisonOmuCaseload(prisonCaseload: List<String>, searchString: String?): CaCaseLoad {
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
        sortBy = "conditionalReleaseDate",
      ),
    )
    val eligibleExistingLicences = filterAndFormatExistingLicences(existingLicences)

    val eligibleNotStartedLicences = findAndFormatNotStartedLicences(
      existingLicences,
      prisonCaseload,
    )

    if (eligibleExistingLicences.isEmpty() && eligibleNotStartedLicences.isEmpty()) {
      return CaCaseLoad(cases = emptyList(), showAttentionNeededTab = false)
    }

    val cases = mapCasesToComs(eligibleExistingLicences + eligibleNotStartedLicences)

    return buildCaseload(cases, searchString, "prison")
  }

  fun getProbationOmuCaseload(
    prisonCaseload: List<String>,
    searchString: String?,
  ): CaCaseLoad {
    val statuses = listOf(
      ACTIVE,
      LicenceStatus.VARIATION_APPROVED,
      LicenceStatus.VARIATION_IN_PROGRESS,
      LicenceStatus.VARIATION_SUBMITTED,
    )
    val licences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        statusCodes = statuses,
        prisonCodes = prisonCaseload,
        sortBy = "conditionalReleaseDate",
      ),
    )

    if (licences.isEmpty()) {
      return CaCaseLoad(cases = emptyList(), showAttentionNeededTab = false)
    }

    val formattedLicences = formatReleasedLicences(licences)
    val cases = mapCasesToComs(formattedLicences)

    return buildCaseload(cases, searchString, "probation")
  }

  private fun formatReleasedLicences(licences: List<LicenceSummary>): List<CaCase> {
    val groupedLicences = licences.groupBy { it.nomisId }
    return groupedLicences.map {
      val licence = if (it.value.size > 1) {
        it.value.find { l -> l.licenceStatus != ACTIVE }
      } else {
        it.value[0]
      }
      val releaseDate = licence?.actualReleaseDate ?: licence?.conditionalReleaseDate
      CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOf,
        name = "${licence?.forename} ${licence?.surname}",
        prisonerNumber = licence?.nomisId!!,
        releaseDate = releaseDate!!,
        releaseDateLabel =
        when (licence.actualReleaseDate) {
          null -> "CRD"
          else -> "Confirmed release date"
        },
        licenceStatus = licence.licenceStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isDueForEarlyRelease = licence.isDueForEarlyRelease,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        probationPractitioner = ProbationPractitioner(
          staffUsername = licence.comUsername,
        ),
      )
    }
  }

  fun mapCasesToComs(casesToMap: List<CaCase>): List<CaCase> {
    val cases = splitCasesByComDetails(casesToMap)

    val noComPrisonerNumbers = cases.withNoComId.map { c -> c.prisonerNumber }
    val deliusRecords = probationSearchApiClient.searchForPeopleByNomsNumber(noComPrisonerNumbers)

    var caCaseList: List<CaCase> = mutableListOf()

    // if no code or username, hit delius to find COM details
    caCaseList += cases.withNoComId.map { caCase ->

      val com =
        deliusRecords.find { d -> d.otherIds.nomsNumber == caCase.prisonerNumber }?.offenderManagers?.find { om -> om.active }?.staffDetail
      if (com != null && !com.unallocated!!) {
        return@map caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.code,
            name = com.let { "${it.forenames} ${it.surname}" },
          ),
        )
      }
      return@map caCase
    }

    // If COM username but no code, do a separate call to use the data in CVL if it exists. Should help highlight any desync between Delius and CVL
    val comUsernames = cases.withStaffUsername.map { c -> c.probationPractitioner?.staffUsername!! }
    val coms = communityApiClient.getStaffDetailsByUsername(comUsernames)

    caCaseList += cases.withStaffUsername.map { caCase ->
      val com =
        coms.find { com -> com.username?.lowercase() == caCase.probationPractitioner?.staffUsername?.lowercase() }
      if (com != null) {
        return@map caCase.copy(
          probationPractitioner = ProbationPractitioner(
            staffCode = com.staffCode,
            name = com.staff?.let { "${it.forenames} ${it.surname}" },
          ),
        )
      }
      return@map caCase
    }

    // If already have COM code and name, no extra calls required
    caCaseList += cases.withStaffCode.map { caCase ->
      return@map caCase.copy(
        probationPractitioner = ProbationPractitioner(
          staffCode = caCase.probationPractitioner?.staffCode,
          name = caCase.probationPractitioner?.name,
        ),
      )
    }

    return caCaseList
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

  fun buildCaseload(cases: List<CaCase>, searchString: String?, view: String): CaCaseLoad {
    val showAttentionNeededTab = cases.any { it.tabType == CaViewCasesTab.ATTENTION_NEEDED }
    val searchResults = applySearch(searchString, cases)
    val sortResults = applySort(searchResults, view)
    return CaCaseLoad(
      cases = sortResults,
      showAttentionNeededTab,
    )
  }

  fun applySearch(searchString: String?, cases: List<CaCase>): List<CaCase> {
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

  fun applySort(cases: List<CaCase>, view: String): List<CaCase> =
    if (view == "prison") cases.sortedBy { it.releaseDate } else cases.sortedByDescending { it.releaseDate }

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
    prisonCaseload: List<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { it.nomisId }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisoner.prisonerNumber)
    }
    val eligiblePrisoners = filterOffendersEligibleForLicence(prisonersWithoutLicences.toList())
    val casesWithoutLicences = pairNomisRecordsWithDelius(eligiblePrisoners)
    return createNotStartedLicenceForCase(casesWithoutLicences)
  }

  private fun createNotStartedLicenceForCase(cases: List<ManagedCase>): List<CaCase> = cases.map { c ->

    // Default status (if not overridden below) will show the case as clickable on case lists
    var licenceStatus = NOT_STARTED

    if (isBreachOfTopUpSupervision(c)) {
      // Imprisonment status indicates a breach of top up supervision order - not clickable (yet)
      licenceStatus = OOS_BOTUS
    } else if (isRecall(c)) {
      // Offender is subject to an active recall - not clickable
      licenceStatus = OOS_RECALL
    } else if (c.cvlFields.isInHardStopPeriod) {
      licenceStatus = TIMED_OUT
    }

    val com = c.deliusRecord?.managedOffenderCrn?.staff

    val releaseDate = c.nomisRecord?.getLicenceStartDate()

    CaCase(
      name = "${c.nomisRecord?.firstName} ${c.nomisRecord?.lastName}",
      prisonerNumber = c.nomisRecord?.prisonerNumber!!,
      releaseDate = releaseDate,
      releaseDateLabel = if (c.nomisRecord.confirmedReleaseDate != null) {
        "Confirmed release date"
      } else {
        "CRD"
      },
      licenceStatus = licenceStatus,
      nomisLegalStatus = c.nomisRecord.legalStatus,
      isDueForEarlyRelease = c.cvlFields.isDueForEarlyRelease,
      isInHardStopPeriod = c.cvlFields.isInHardStopPeriod,
      tabType = determineCaViewCasesTab(c.nomisRecord, c.cvlFields, licence = null),
      probationPractitioner = ProbationPractitioner(
        staffCode = com?.code,
        name = com?.let { "${it.forenames} ${it.surname}" },
      ),
    )
  }

  private fun filterOffendersEligibleForLicence(offenders: List<CaseloadItem>): List<CaseloadItem> {
    val eligibleOffenders = offenders.filter { eligibilityService.isEligibleForCvl(it.prisoner) }

    if (eligibleOffenders.isEmpty()) return eligibleOffenders

    val hdcStatuses = prisonApiClient.getHdcStatuses(
      eligibleOffenders.mapNotNull { c -> c.prisoner.bookingId?.toLong() },
    )

    return eligibleOffenders.filter {
      val hdcRecord = hdcStatuses.find { hdc -> hdc.bookingId == it.prisoner.bookingId?.toLong() }
      return@filter (
        hdcRecord == null ||
          hdcRecord.approvalStatus != "APPROVED" ||
          it.prisoner.homeDetentionCurfewEligibilityDate == null
        )
    }
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> =
    licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun enrichWithNomisData(licences: List<LicenceSummary>, prisoners: List<CaseloadItem>): List<CaCase> {
    return prisoners.map { p ->
      val licencesForOffender = licences.filter { l -> l.nomisId == p.prisoner.prisonerNumber }
      if (licencesForOffender.isEmpty()) return@map null
      val licence = findLatestLicenceSummary(licencesForOffender)
      val releaseDate = licence?.actualReleaseDate ?: licence?.conditionalReleaseDate
      return@map CaCase(
        kind = licence?.kind,
        licenceId = licence?.licenceId,
        licenceVersionOf = licence?.versionOf,
        name = licence.let { "${it?.forename} ${it?.surname}" },
        prisonerNumber = licence?.nomisId!!,
        releaseDate = releaseDate,
        releaseDateLabel = if (licence.actualReleaseDate != null) {
          "Confirmed release date"
        } else {
          "CRD"
        },
        licenceStatus = licence.licenceStatus,
        nomisLegalStatus = p.prisoner.legalStatus,
        lastWorkedOnBy = licence.updatedByFullName,
        isDueForEarlyRelease = licence.isDueForEarlyRelease,
        isInHardStopPeriod = licence.isInHardStopPeriod,
        tabType = determineCaViewCasesTab(
          p.prisoner,
          p.cvl,
          licence,
        ),
        probationPractitioner = ProbationPractitioner(staffUsername = licence.comUsername),
      )
    }.filterNotNull()
  }

  private fun determineCaViewCasesTab(
    nomisRecord: PrisonerSearchPrisoner,
    cvlFields: CvlFields,
    licence: LicenceSummary?,
  ): CaViewCasesTab {
    val releaseDate =
      (licence?.actualReleaseDate ?: licence?.conditionalReleaseDate) ?: nomisRecord.getLicenceStartDate()

    if (licence != null &&
      isAttentionNeeded(
        licence.licenceStatus,
        licence.licenceStartDate,
        releaseDate,
      )
    ) {
      return CaViewCasesTab.ATTENTION_NEEDED
    }
    val isDueToBeReleasedInTheNextTwoWorkingDays =
      licence?.isDueToBeReleasedInTheNextTwoWorkingDays ?: cvlFields.isDueToBeReleasedInTheNextTwoWorkingDays

    return when {
      isDueToBeReleasedInTheNextTwoWorkingDays -> CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS
      else -> CaViewCasesTab.FUTURE_RELEASES
    }
  }

  private fun isAttentionNeeded(
    status: LicenceStatus,
    licenceStartDate: LocalDate?,
    releaseDate: LocalDate?,
    overrideClock: Clock? = null,
  ): Boolean {
    val now = overrideClock ?: clock
    val today = LocalDate.now(now)

    val noReleaseDates = releaseDate == null

    val missingDates = listOf(
      IN_PROGRESS,
      SUBMITTED,
      APPROVED,
      NOT_STARTED,
    ).contains(status) &&
      noReleaseDates
    val startDateInPast = licenceStartDate != null && status == APPROVED && licenceStartDate.isBefore(today)

    return missingDates || startDateInPast
  }

  private fun getPrisonersApproachingRelease(
    prisonCaseload: List<String>,
    overrideClock: Clock? = null,
  ): Page<CaseloadItem> {
    val now = overrideClock ?: clock
    val weeksToAdd: Long = 4
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(weeksToAdd)
    return prisonCaseload.toSet().let {
      caseloadService.getPrisonersByReleaseDate(
        today,
        todayPlusFourWeeks,
        it,
        page = 0,
      )
    }
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

  private fun pairNomisRecordsWithDelius(prisoners: List<CaseloadItem>): List<ManagedCase> {
    val caseloadNomisIds = prisoners
      .map { it.prisoner.prisonerNumber }

    val deliusRecords = probationSearchApiClient.searchForPeopleByNomsNumber(caseloadNomisIds)

    return prisoners
      .map { (prisoner, cvl) ->
        val deliusRecord = deliusRecords.find { d -> d.otherIds.nomsNumber == prisoner.prisonerNumber }
        if (deliusRecord != null) {
          return@map ManagedCase(
            nomisRecord = prisoner,
            cvlFields = cvl,
            deliusRecord = DeliusRecord(
              deliusRecord,
              ManagedOffenderCrn(
                staff = deliusRecord.offenderManagers.find { om -> om.active }?.staffDetail,
              ),
            ),
          )
        }
        return@map ManagedCase(nomisRecord = prisoner, cvlFields = cvl)
      }
  }
}
