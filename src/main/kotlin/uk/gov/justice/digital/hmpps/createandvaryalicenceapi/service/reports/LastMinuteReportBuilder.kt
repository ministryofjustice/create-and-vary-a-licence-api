package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import java.time.Clock
import java.time.LocalDate

// Staged builder interfaces to force order of operations
private interface DeliusStage {
  fun enrichWithDeliusData(): CvlRecordStage
}

private interface CvlRecordStage {
  fun enrichWithCvlRecords(): EligibleCandidatesStage
}

private interface EligibleCandidatesStage {
  fun withEligibleCandidates(): PreSubmissionStage
}

private interface PreSubmissionStage {
  fun withPreSubmissionState(): HdcFilterStage
}

private interface HdcFilterStage {
  fun filterOutHdcEligible(): LicenceStartStage
}

private interface LicenceStartStage {
  fun filterByLicenceStartDates(): BuildStage
}

private interface BuildStage {
  fun build(): List<LastMinuteHandoverCaseResponse>
}

class LastMinuteReportBuilder(
  private val licenceRepository: LicenceRepository,
  private val hdcService: HdcService,
  private val deliusApiClient: DeliusApiClient,
  private val cvlRecordService: CvlRecordService,
  private val clock: Clock,
) : CvlRecordStage,
  DeliusStage,
  EligibleCandidatesStage,
  PreSubmissionStage,
  HdcFilterStage,
  LicenceStartStage,
  BuildStage {

  private lateinit var prisoners: Map<String, PrisonerSearchPrisoner>
  private lateinit var inProgressEligiblePrisoners: Set<String>
  private lateinit var eligiblePrisoners: Set<String>
  private lateinit var licenceStartDates: Map<String, LocalDate>
  private lateinit var deliusData: Map<String, CommunityManager>
  private lateinit var candidates: Map<String, PrisonerSearchPrisoner>
  private lateinit var cvlRecords: List<CvlRecord>

  fun start(prisoners: Map<String, PrisonerSearchPrisoner>) = apply {
    this.prisoners = prisoners
    this.candidates = emptyMap()
    this.inProgressEligiblePrisoners = emptySet()
    this.cvlRecords = emptyList()
    this.deliusData = emptyMap()
  }

  override fun withEligibleCandidates() = apply {
    candidates = prisoners.filter { (nomisId, _) ->
      val cvlRecord = cvlRecords.find { cvlRecord -> cvlRecord.nomisId == nomisId }
      return@filter cvlRecord?.isEligible == true
    }
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
    val nomisIds = prisoners.keys.toList()

    val deliusRecords = deliusApiClient.getOffenderManagers(nomisIds)
      .mapNotNull { it.case.nomisId?.let { nomisId -> nomisId to it } }
      .toMap()

    deliusData = nomisIds.mapNotNull { nomisId ->
      deliusRecords[nomisId]?.let { nomisId to it }
    }.toMap()
  }

  override fun enrichWithCvlRecords() = apply {
    cvlRecords = cvlRecordService.getCvlRecords(prisoners.values.toList())
  }

  override fun filterByLicenceStartDates() = apply {
    val today = LocalDate.now(clock)
    val nextWeek = today.plusWeeks(1)
    val dates = cvlRecords.map { it.nomisId to it.licenceStartDate }
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
      prisonName = prisoner.prisonName,
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
