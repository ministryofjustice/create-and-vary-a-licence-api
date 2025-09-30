package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import java.time.Clock
import java.time.LocalDate

@Service
class LastMinuteHandoverCaseService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val eligibilityService: EligibilityService,
  private val licenceRepository: LicenceRepository,
  private val hdcService: HdcService,
  private val deliusApiClient: DeliusApiClient,
  private val releaseDateService: ReleaseDateService,
  @param:Value("\${hmpps.cases.last-minute.prisons}")
  private val lastMinutePrisonCodes: List<String> = listOf(),
  private val overrideClock: Clock = Clock.systemDefaultZone(),
) {

  private data class LastMinuteReportData(
    val prisoners: Map<String, PrisonerSearchPrisoner> = mapOf(),
  ) {
    var inProgressEligiblePrisoners: Set<String> = setOf()
    var eligiblePrisoners: Set<String> = setOf()
    var licenceStartDates: Map<String, LocalDate> = mapOf()
    var deliusData: Map<String, CommunityManager> = mapOf()
    var candidates: Map<String, PrisonerSearchPrisoner> = mapOf()
  }

  fun getLastMinuteCases(): List<LastMinuteHandoverCaseResponse> {
    log.info("Getting last minute handover cases")

    val lastMinuteReportData = LastMinuteReportData(getPrisonerData())
    processIneligibleCases(lastMinuteReportData)
    processPreSubmissionState(lastMinuteReportData)
    excludePrisonersWithHdc(lastMinuteReportData)
    addDeliusOffenderManagerData(lastMinuteReportData)
    processLicenceStartDates(lastMinuteReportData)

    with(lastMinuteReportData) {
      return candidates.map { candidate ->
        createTagReportCaseResponse(candidate)
      }.sortedBy {
        it.releaseDate
        it.prisonerName
      }
    }
  }

  private fun getPrisonerData(): Map<String, PrisonerSearchPrisoner> {
    val today = getToday()
    val nextWeek = getNextWeek()
    log.info("Getting prisoner data for $today <> $nextWeek prisons : $lastMinutePrisonCodes")
    val prisoners = prisonerSearchApiClient.searchPrisonersByReleaseDate(today, nextWeek, lastMinutePrisonCodes)
    log.info("Found ${prisoners.size} prisoners")
    return prisoners.associateBy { it.prisonerNumber }
  }

  private fun getToday(): LocalDate = LocalDate.now(overrideClock)
  private fun getNextWeek(): LocalDate = getToday().plusWeeks(1)

  private fun processIneligibleCases(lastMinuteReportData: LastMinuteReportData) {
    log.info("Processing ineligible cases")
    with(lastMinuteReportData) {
      candidates = prisoners.filter { eligibilityService.isEligibleForCvl(it.value) }
    }
  }

  private fun processPreSubmissionState(lastMinuteReportData: LastMinuteReportData) {
    log.info("Processing pre-submission cases")
    with(lastMinuteReportData) {
      val prisonerNumbers = candidates.keys.toList()
      val results = licenceRepository.findStatesByPrisonNumbers(prisonerNumbers)
      log.info("Found ${prisonerNumbers.size}  prisoner results")
      val inProgress = results.filter { it.statusCode == IN_PROGRESS }
        .map { it.prisonNumber }
        .toSet()

      val notStarted = prisonerNumbers - results.map { it.prisonNumber }.toSet()

      inProgressEligiblePrisoners = inProgress
      eligiblePrisoners = inProgress + notStarted
      candidates =
        candidates.filter { eligiblePrisoners.contains(it.value.prisonerNumber) }
    }
  }

  private fun excludePrisonersWithHdc(lastMinuteReportData: LastMinuteReportData) {
    log.info("Excluding prisoners with HDC")
    with(lastMinuteReportData) {
      val prisoners = candidates.values.toList()
      val hdcStatuses = hdcService.getHdcStatus(prisoners)
      candidates = candidates.filterNot { hdcStatuses.isApprovedForHdc(it.value.bookingId?.toLong()!!) }
    }
  }

  private fun addDeliusOffenderManagerData(lastMinuteReportData: LastMinuteReportData) {
    with(lastMinuteReportData) {
      val prisoners = candidates.values
      log.info("Adding delius data ${prisoners.size} prisoners")

      val result = deliusApiClient.getOffenderManagers(prisoners.map { it.prisonerNumber })
      val prisonNumberOffenderManagerMap = result.filter { it.case.nomisId != null }.associateBy { it.case.nomisId!! }
      log.info("Found ${prisonNumberOffenderManagerMap.size} delius records")

      deliusData = prisoners.mapNotNull { prisoner ->
        prisonNumberOffenderManagerMap[prisoner.prisonerNumber]?.let { prisoner.prisonerNumber to it }
      }.toMap()
    }
  }

  private fun processLicenceStartDates(lastMinuteReportData: LastMinuteReportData) {
    log.info("Processing licence start dates")
    with(lastMinuteReportData) {
      val startDate = getToday()
      val endDate = getNextWeek()
      val prisoners = candidates.values.toList()

      val lsd = releaseDateService.getLicenceStartDates(prisoners)
        .mapNotNull { (key, value) -> value?.let { key to it } }
        .toMap()
        .filterValues { it in startDate..endDate }

      licenceStartDates = lsd
      candidates = candidates.filter { licenceStartDates.containsKey(it.key) }
    }
  }

  private fun LastMinuteReportData.createTagReportCaseResponse(
    candidate: Map.Entry<String, PrisonerSearchPrisoner>,
  ): LastMinuteHandoverCaseResponse {
    val communityManager = deliusData[candidate.key]
    val prisoner = candidate.value

    return LastMinuteHandoverCaseResponse(
      releaseDate = licenceStartDates[candidate.key]!!,
      prisonerNumber = prisoner.prisonerNumber,
      prisonCode = prisoner.prisonId,
      prisonerName = prisoner.fullName(),
      crn = communityManager?.case?.crn,
      probationRegion = communityManager?.team?.district?.code,
      probationPractitioner = communityManager?.name?.fullName(),
      status = getStatus(candidate),
    )
  }

  private fun LastMinuteReportData.getStatus(
    candidate: Map.Entry<String, PrisonerSearchPrisoner>,
  ) = if (inProgressEligiblePrisoners.contains(candidate.key)) IN_PROGRESS else NOT_STARTED

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
