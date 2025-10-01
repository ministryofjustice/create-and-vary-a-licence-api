package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import java.time.Clock
import java.time.LocalDate
import kotlin.comparisons.nullsLast

// Staged builder interfaces to force order of operations
private interface EligibleCandidatesStage {
  fun withEligibleCandidates(): PreSubmissionStage
}
private interface PreSubmissionStage {
  fun withPreSubmissionState(): HdcFilterStage
}
private interface HdcFilterStage {
  fun filterOutHdcEligible(): DeliusStage
}
private interface DeliusStage {
  fun enrichWithDeliusData(): LicenceStartStage
}
private interface LicenceStartStage {
  fun filterByLicenceStartDates(): BuildStage
}
private interface BuildStage {
  fun build(): List<LastMinuteHandoverCaseResponse>
}

class LastMinuteReportBuilder(
  private val eligibilityService: EligibilityService,
  private val licenceRepository: LicenceRepository,
  private val hdcService: HdcService,
  private val deliusApiClient: DeliusApiClient,
  private val releaseDateService: ReleaseDateService,
  private val clock: Clock,
) : EligibleCandidatesStage,
  PreSubmissionStage,
  HdcFilterStage,
  DeliusStage,
  LicenceStartStage,
  BuildStage {

  private lateinit var prisoners: Map<String, PrisonerSearchPrisoner>
  private lateinit var inProgressEligiblePrisoners: Set<String>
  private lateinit var eligiblePrisoners: Set<String>
  private lateinit var licenceStartDates: Map<String, LocalDate>
  private lateinit var deliusData: Map<String, CommunityManager>
  private lateinit var candidates: Map<String, PrisonerSearchPrisoner>

  fun start(prisoners: Map<String, PrisonerSearchPrisoner>) = apply {
    this.prisoners = prisoners
    this.candidates = emptyMap()
    this.inProgressEligiblePrisoners = emptySet()
    this.licenceStartDates = emptyMap()
    this.deliusData = emptyMap()
  }

  override fun withEligibleCandidates() = apply {
    candidates = prisoners.filter { eligibilityService.isEligibleForCvl(it.value) }
  }

  override fun withPreSubmissionState() = apply {
    val prisonerNumbers = candidates.keys
    val results = licenceRepository.findStatesByPrisonNumbers(prisonerNumbers.toList())
    val inProgress = results.filter { it.statusCode == IN_PROGRESS }.map { it.prisonNumber }.toSet()
    val notStarted = prisonerNumbers - results.map { it.prisonNumber }.toSet()
    val eligible = inProgress + notStarted
    inProgressEligiblePrisoners = inProgress
    eligiblePrisoners = eligible
    candidates = candidates.filterKeys { it in eligible }
  }

  override fun filterOutHdcEligible() = apply {
    candidates = candidates.filterValues {
      it.bookingId?.let { id -> !hdcService.getHdcStatus(listOf(it)).isApprovedForHdc(id.toLong()) } ?: true
    }
  }

  override fun enrichWithDeliusData() = apply {
    val prisonersList = candidates.values.toList()

    val deliusRecords = deliusApiClient.getOffenderManagers(prisonersList.map { it.prisonerNumber })
      .mapNotNull { it.case.nomisId?.let { nomisId -> nomisId to it } }
      .toMap()

    deliusData = prisonersList.mapNotNull { prisoner ->
      deliusRecords[prisoner.prisonerNumber]?.let { prisoner.prisonerNumber to it }
    }.toMap()
  }

  override fun filterByLicenceStartDates() = apply {
    val today = LocalDate.now(clock)
    val nextWeek = today.plusWeeks(1)
    val dates = releaseDateService.getLicenceStartDates(candidates.values.toList())
      .mapNotNull { (key, date) -> date?.takeIf { it in today..nextWeek }?.let { key to it } }
      .toMap()
    licenceStartDates = dates
    candidates = candidates.filterKeys { it in dates.keys }
  }

  override fun build(): List<LastMinuteHandoverCaseResponse> = candidates.map { (prisonerNumber, prisoner) ->
    val communityManager = deliusData[prisonerNumber]
    val releaseDate = requireNotNull(licenceStartDates[prisonerNumber]) {
      "Licence start date missing for prisoner $prisonerNumber"
    }
    val licenceStatus = if (prisonerNumber in inProgressEligiblePrisoners) IN_PROGRESS else NOT_STARTED
    LastMinuteHandoverCaseResponse(
      releaseDate = releaseDate,
      prisonerNumber = prisonerNumber,
      prisonCode = prisoner.prisonId,
      prisonerName = prisoner.fullName(),
      crn = communityManager?.case?.crn,
      probationRegion = communityManager?.provider?.description,
      probationPractitioner = communityManager?.name?.fullName(),
      status = licenceStatus,
    )
  }.sortedWith(sortResponse())

  private fun sortResponse() = compareBy<LastMinuteHandoverCaseResponse, String?>(nullsLast()) { it.probationRegion }
    .thenBy { it.prisonCode }
    .thenBy { it.releaseDate }
}
