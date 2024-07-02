package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.*
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CaseloadService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.LocalDate

@Service
class CaCaseloadService(
  val caseloadService: CaseloadService,
  private val clock: Clock,
  private val probationSearchApiClient: ProbationSearchApiClient,
) {
  fun getPrisonView(prisons: List<String>, searchString: String): List<CaCaseLoad> = emptyList()
  fun getProbationView(prisons: List<String>, searchString: String): List<CaCaseLoad> = emptyList()

  private fun getPrisonersApproachingRelease(
    prisonCaseload: Set<String>,
    overrideClock: Clock? = null,
  ): Page<CaseloadItem> {
    val now = overrideClock ?: clock
    val weeksToAdd: Long = 4
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(weeksToAdd)
    return caseloadService.getPrisonersByReleaseDate(
      today,
      todayPlusFourWeeks,
      prisonCaseload,
      page = 0,
    )
  }

  private fun getCasesWithoutLicences(cases: List<CaseloadItem>, licenceNomisIds: List<String>): List<ManagedCase> {
    val casesWithoutLicences = cases.filter { it -> !licenceNomisIds.contains(it.prisoner.prisonerNumber) }
    return pairNomisRecordsWithDelius(casesWithoutLicences)
  }

  private fun buildCaseload(cases: List<CaCase>, searchString: String, view: String): CaCaseLoad {
    val showAttentionNeededTab = cases.any { it -> it.tabType == CaViewCasesTab.ATTENTION_NEEDED }
    val searchResults = applySearch(searchString, cases)
    val sortResults = applySort(searchResults, view)
    return CaCaseLoad(
      cases = sortResults,
      showAttentionNeededTab,
    )
  }

  private fun isPastRelease(licence: LicenceSummary, overrideClock: Clock? = null): Boolean {
    val now = overrideClock ?: clock
    val today = LocalDate.now(now)
    val releaseDate = licence.actualReleaseDate ?: licence.conditionalReleaseDate
    if (releaseDate != null) {
      return releaseDate < today
    }
    return false
  }

  private fun findLatestLicenceSummary(licences: List<LicenceSummary>): LicenceSummary? {
    if (licences.size == 1) {
      return licences[0]
    }
    if (licences.any { it -> it.licenceStatus == LicenceStatus.TIMED_OUT }) {
      return licences.find { it -> it.licenceStatus != LicenceStatus.TIMED_OUT }
    }

    return licences.find { it ->
      (it.licenceStatus == LicenceStatus.SUBMITTED) || (it.licenceStatus == LicenceStatus.IN_PROGRESS)
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
        it.probationPractitioner!!.name.lowercase().contains(term)
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
}
