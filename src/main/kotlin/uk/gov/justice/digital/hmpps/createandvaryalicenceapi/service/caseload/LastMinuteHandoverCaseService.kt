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
  private val lastMinutePrisonCodes: List<String> = emptyList(),
  private val overrideClock: Clock = Clock.systemDefaultZone(),
) {

  private data class LastMinuteReportDataBuilder(
    val prisoners: Map<String, PrisonerSearchPrisoner>,
    val inProgressEligiblePrisoners: Set<String> = emptySet(),
    val eligiblePrisoners: Set<String> = emptySet(),
    val licenceStartDates: Map<String, LocalDate> = emptyMap(),
    val deliusData: Map<String, CommunityManager> = emptyMap(),
    val candidates: Map<String, PrisonerSearchPrisoner> = emptyMap(),
  )

  fun getLastMinuteCases(): List<LastMinuteHandoverCaseResponse> {
    log.info("Getting last minute handover cases")

    val result = LastMinuteReportDataBuilder(getPrisonerData())
      .withEligibleCandidates()
      .withPreSubmissionState()
      .withoutHdc()
      .withDeliusData()
      .withLicenceStartDates()

    return result.toResponses()
      .sortedWith(
        compareBy<LastMinuteHandoverCaseResponse> { it.releaseDate }
          .thenBy { it.prisonerName },
      )
  }

  private fun getPrisonerData(): Map<String, PrisonerSearchPrisoner> {
    val today = LocalDate.now(overrideClock)
    val nextWeek = today.plusWeeks(1)
    log.info("Getting prisoner data for $today <> $nextWeek prisons : $lastMinutePrisonCodes")
    val prisoners = prisonerSearchApiClient.searchPrisonersByReleaseDate(today, nextWeek, lastMinutePrisonCodes)
    log.info("Found ${prisoners.size} prisoners")
    return prisoners.associateBy { it.prisonerNumber }
  }

  private fun LastMinuteReportDataBuilder.withEligibleCandidates(): LastMinuteReportDataBuilder {
    val candidates = prisoners.filter { eligibilityService.isEligibleForCvl(it.value) }
    return copy(candidates = candidates)
  }

  private fun LastMinuteReportDataBuilder.withPreSubmissionState(): LastMinuteReportDataBuilder {
    val prisonerNumbers = candidates.keys
    val results = licenceRepository.findStatesByPrisonNumbers(prisonerNumbers.toList())

    val inProgress = results.filter { it.statusCode == IN_PROGRESS }.map { it.prisonNumber }.toSet()
    val notStarted = prisonerNumbers - results.map { it.prisonNumber }.toSet()
    val eligible = inProgress + notStarted

    val filteredCandidates = candidates.filterKeys { it in eligible }
    return copy(
      inProgressEligiblePrisoners = inProgress,
      eligiblePrisoners = eligible,
      candidates = filteredCandidates,
    )
  }

  private fun LastMinuteReportDataBuilder.withoutHdc(): LastMinuteReportDataBuilder {
    val prisonersList = candidates.values.toList()
    val hdcStatuses = hdcService.getHdcStatus(prisonersList)

    val filteredCandidates = candidates.filterValues {
      it.bookingId?.let { id -> !hdcStatuses.isApprovedForHdc(id.toLong()) } ?: true
    }
    return copy(candidates = filteredCandidates)
  }

  private fun LastMinuteReportDataBuilder.withDeliusData(): LastMinuteReportDataBuilder {
    val prisonersList = candidates.values.toList()
    log.info("Adding Delius data for ${prisonersList.size} prisoners")

    val deliusRecords = deliusApiClient.getOffenderManagers(prisonersList.map { it.prisonerNumber })
      .mapNotNull { it.case.nomisId?.let { nomisId -> nomisId to it } }
      .toMap()

    val filteredDeliusData = prisonersList.mapNotNull { prisoner ->
      deliusRecords[prisoner.prisonerNumber]?.let { prisoner.prisonerNumber to it }
    }.toMap()

    log.info("Found ${filteredDeliusData.size} Delius records")
    return copy(deliusData = filteredDeliusData)
  }

  private fun LastMinuteReportDataBuilder.withLicenceStartDates(): LastMinuteReportDataBuilder {
    val today = LocalDate.now(overrideClock)
    val nextWeek = today.plusWeeks(1)
    val prisonersList = candidates.values.toList()

    val licenceStartDates = releaseDateService.getLicenceStartDates(prisonersList)
      .mapNotNull { (key, date) -> date?.takeIf { it in today..nextWeek }?.let { key to it } }
      .toMap()

    val filteredCandidates = candidates.filterKeys { it in licenceStartDates.keys }

    return copy(licenceStartDates = licenceStartDates, candidates = filteredCandidates)
  }

  private fun LastMinuteReportDataBuilder.toResponses(): List<LastMinuteHandoverCaseResponse> =
    candidates.map { (prisonerNumber, prisoner) ->
      val communityManager = deliusData[prisonerNumber]
      val releaseDate = requireNotNull(licenceStartDates[prisonerNumber]) {
        "Licence start date missing for prisoner $prisonerNumber"
      }
      val status = if (prisonerNumber in inProgressEligiblePrisoners) IN_PROGRESS else NOT_STARTED

      LastMinuteHandoverCaseResponse(
        releaseDate = releaseDate,
        prisonerNumber = prisonerNumber,
        prisonCode = prisoner.prisonId,
        prisonerName = prisoner.fullName(),
        crn = communityManager?.case?.crn,
        probationRegion = communityManager?.provider?.description,
        probationPractitioner = communityManager?.name?.fullName(),
        status = status
      )
    }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
