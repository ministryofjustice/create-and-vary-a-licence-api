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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.VARIATION_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isBreachOfTopUpSupervision
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isEligibleEDS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isParoleEligible
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isRecall
import java.time.Clock
import java.time.LocalDate

@Service
class CaCaseloadService(
  val caseloadService: CaseloadService,
  private val clock: Clock,
  private val probationSearchApiClient: ProbationSearchApiClient,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
  private val communityApiClient: CommunityApiClient,
) {
  fun getPrisonOmuCaseload(prisonCaseload: List<String>, searchString: String?): CaCaseLoad {
    val existingLicences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        prisonCodes = prisonCaseload,
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

    return buildCaseload(cases, searchString!!, "prison")
  }

  fun getProbationOmuCaseload(
    prisonCaseload: List<String>,
    searchString: String?,
  ): CaCaseLoad {
    val statuses = listOf(
      ACTIVE,
      VARIATION_APPROVED,
      VARIATION_IN_PROGRESS,
      VARIATION_SUBMITTED,
    )
    val licences = licenceService.findLicencesMatchingCriteria(
      LicenceQueryObject(
        statusCodes = statuses,
        prisonCodes = prisonCaseload,
      ),
    )

    if (licences.isEmpty()) {
      return CaCaseLoad(cases = emptyList(), showAttentionNeededTab = false)
    }

    val formattedLicences = formatReleasedLicences(licences)
    val cases = mapCasesToComs(formattedLicences)

    return buildCaseload(cases, searchString, "probation")
  }

  private fun filterAndFormatExistingLicences(licences: List<LicenceSummary>): List<CaCase> {
    if (licences.isEmpty()) {
      return emptyList()
    }

    val licenceNomisIds = licences.map { it -> it.nomisId }
    val prisonersWithLicences = caseloadService.getPrisonersByNumber(licenceNomisIds)
    val nomisEnrichedLicences = this.enrichWithNomisData(licences, prisonersWithLicences)
    return filterExistingLicencesForEligibility(nomisEnrichedLicences)
  }

  private fun findAndFormatNotStartedLicences(
    licences: List<LicenceSummary>,
    prisonCaseload: List<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { l -> l.nomisId }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    if (prisonersApproachingRelease.size == 0) {
      return emptyList()
    }

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisoner.prisonerNumber)
    }
    val eligiblePrisoners = filterOffendersEligibleForLicence(prisonersWithoutLicences.toList())
    val casesWithoutLicences = pairNomisRecordsWithDelius(eligiblePrisoners)
    return createNotStartedLicenceForCase(casesWithoutLicences)
  }

  private fun formatReleasedLicences(licences: List<LicenceSummary>): List<CaCase> {
    val groupedLicences = licences.groupBy { it.nomisId }
    return groupedLicences.map { it ->

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
        releaseDate = releaseDate?.toString() ?: "not found",
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

  private fun splitCasesByComDetails(cases: List<CaCase>): GroupedByCom = cases.fold(
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
    return acc
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
      licenceStatus = LicenceStatus.TIMED_OUT
    }

    val com = c.deliusRecord?.managedOffenderCrn?.staff

    if (c.nomisRecord?.conditionalReleaseDate == null) {
      CaCase(
        name = "${c.nomisRecord?.firstName} ${c.nomisRecord?.lastName}",
        prisonerNumber = c.nomisRecord?.prisonerNumber!!,
        releaseDate = null,
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
          name = "${com?.forenames} ${com?.surname}",
        ),
      )
    }

    val releaseDate = c.nomisRecord.confirmedReleaseDate ?: c.nomisRecord.conditionalReleaseDate

    CaCase(
      name = "${c.nomisRecord.firstName} ${c.nomisRecord.lastName}",
      prisonerNumber = c.nomisRecord.prisonerNumber!!,
      releaseDate = releaseDate.toString(),
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
        name = "${com?.forenames} ${com?.surname}",
      ),
    )
  }

  private fun filterOffendersEligibleForLicence(offenders: List<CaseloadItem>): List<CaseloadItem> {
    val eligibleOffenders = offenders
      .filter { offender -> !isParoleEligible(offender.prisoner.paroleEligibilityDate!!) }
      .filter { offender -> offender.prisoner.legalStatus != "DEAD" }
      .filter { offender -> !offender.prisoner.indeterminateSentence!! }
      .filter { offender -> offender.prisoner.conditionalReleaseDate != null }
      .filter { offender ->
        isEligibleEDS(
          offender.prisoner.paroleEligibilityDate,
          offender.prisoner.conditionalReleaseDate,
          offender.prisoner.confirmedReleaseDate,
          offender.prisoner.actualParoleDate,
        )
      }

    if (eligibleOffenders.isEmpty()) return eligibleOffenders

    val hdcStatuses = prisonApiClient.getHdcStatuses(
      eligibleOffenders.map { c -> c.prisoner.bookingId!!.toLong() },
    )

    return eligibleOffenders.filter { it ->
      val hdcRecord = hdcStatuses.find { hdc -> hdc.bookingId == it.prisoner.bookingId?.toLong() }
      return@filter (
        hdcRecord?.approvalStatus != "APPROVED" ||
          it.prisoner.homeDetentionCurfewEligibilityDate != null
        )
    }
  }

  private fun filterExistingLicencesForEligibility(licences: List<CaCase>): List<CaCase> =
    licences.filter { l -> l.nomisLegalStatus != "DEAD" }

  private fun enrichWithNomisData(licences: List<LicenceSummary>, prisoners: List<CaseloadItem>): List<CaCase> {
    return prisoners.map<CaseloadItem, CaCase> { p ->

      val licencesForOffender = licences.filter { l -> l.nomisId === p.prisoner.prisonerNumber }
      val licence = findLatestLicenceSummary(licencesForOffender)
      val releaseDate = licence?.actualReleaseDate ?: licence?.conditionalReleaseDate
      return listOf(
        CaCase(
          kind = licence?.kind,
          licenceId = licence?.licenceId,
          licenceVersionOf = licence?.versionOf,
          name = "${licence?.forename} ${licence?.surname}",
          prisonerNumber = licence?.nomisId!!,
          releaseDate = releaseDate?.toString() ?: "not found",
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
        ),
      )
    }
  }

  private fun determineCaViewCasesTab(
    nomisRecord: Prisoner,
    cvlFields: CvlFields,
    licence: LicenceSummary?,
  ): CaViewCasesTab {
    val releaseDate = (licence?.actualReleaseDate ?: licence?.conditionalReleaseDate) ?: selectReleaseDate(
      nomisRecord,
    )

    if (licence != null &&
      isAttentionNeeded(
        licence.licenceStatus,
        licence.licenceStartDate!!,
        releaseDate!!,
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

  private fun selectReleaseDate(nomisRecord: Prisoner): LocalDate? {
    val dateString = nomisRecord.confirmedReleaseDate ?: nomisRecord.conditionalReleaseDate

    if (dateString == null) {
      return null
    }

    return dateString
  }

  private fun getPrisonersApproachingRelease(
    prisonCaseload: List<String>,
    overrideClock: Clock? = null,
  ): Page<CaseloadItem> {
    val now = overrideClock ?: clock
    val weeksToAdd: Long = 4
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(weeksToAdd)
    return caseloadService.getPrisonersByReleaseDate(
      today,
      todayPlusFourWeeks,
      prisonCaseload.toSet(),
      page = 0,
    )
  }

  private fun findLatestLicenceSummary(licences: List<LicenceSummary>): LicenceSummary? {
    if (licences.size == 1) {
      return licences[0]
    }
    if (licences.any { it -> it.licenceStatus == LicenceStatus.TIMED_OUT }) {
      return licences.find { it -> it.licenceStatus != LicenceStatus.TIMED_OUT }
    }

    return licences.find { it ->
      (it.licenceStatus == SUBMITTED) || (it.licenceStatus == IN_PROGRESS)
    }
  }

  private fun applySearch(searchString: String?, cases: List<CaCase>): List<CaCase> {
    if (searchString == null) {
      return cases
    }
    val term = searchString.lowercase()
    return cases.filter { it ->
      it.name.lowercase().contains(term) ||
        it.prisonerNumber.lowercase().contains(term) ||
        it.probationPractitioner?.name?.lowercase()!!.contains(term)
    }
  }

  private fun applySort(cases: List<CaCase>, view: String): List<CaCase> =
    if (view == "prison") cases.sortedBy { it -> it.releaseDate } else cases.sortedByDescending { it -> it.releaseDate }

  private fun pairNomisRecordsWithDelius(prisoners: List<CaseloadItem>): List<ManagedCase> {
    val caseloadNomisIds = prisoners
      .map { it -> it.prisoner.prisonerNumber!! }

    val deliusRecords = probationSearchApiClient.searchForPeopleByNomsNumber(caseloadNomisIds)

    return prisoners
      .map { (prisoner, cvl) ->
        val deliusRecord = deliusRecords.find { d -> d.otherIds.nomsNumber == prisoner.prisonerNumber }
        if (deliusRecord != null) {
          return listOf(
            ManagedCase(
              nomisRecord = prisoner,
              cvlFields = cvl,
              deliusRecord = DeliusRecord(
                deliusRecord,
                ManagedOffenderCrn(
                  staff = deliusRecord.offenderManagers.find { om -> om.active }?.staffDetail,
                ),
              ),
            ),
          )
        }
        return listOf(ManagedCase(nomisRecord = prisoner, cvlFields = cvl))
      }
  }

  private fun mapCasesToComs(casesToMap: List<CaCase>): List<CaCase> {
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
            name = "${com.forenames} ${com.surname}",
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
            name = "${com.staff?.forenames} ${com.staff?.surname}",
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

  private fun buildCaseload(cases: List<CaCase>, searchString: String?, view: String): CaCaseLoad {
    val showAttentionNeededTab = cases.any { it -> it.tabType == CaViewCasesTab.ATTENTION_NEEDED }
    val searchResults = applySearch(searchString, cases)
    val sortResults = applySort(searchResults, view)
    return CaCaseLoad(
      cases = sortResults,
      showAttentionNeededTab,
    )
  }

  //  private fun isPastRelease(licence: LicenceSummary, overrideClock: Clock? = null): Boolean {
//    val now = overrideClock ?: clock
//    val today = LocalDate.now(now)
//    val releaseDate = licence.actualReleaseDate ?: licence.conditionalReleaseDate
//    if (releaseDate != null) {
//      return releaseDate < today
//    }
//    return false
//  }

  //  private fun getCasesWithoutLicences(cases: List<CaseloadItem>, licenceNomisIds: List<String>): List<ManagedCase> {
//    val casesWithoutLicences = cases.filter { it -> !licenceNomisIds.contains(it.prisoner.prisonerNumber) }
//    return pairNomisRecordsWithDelius(casesWithoutLicences)
//  }
}
