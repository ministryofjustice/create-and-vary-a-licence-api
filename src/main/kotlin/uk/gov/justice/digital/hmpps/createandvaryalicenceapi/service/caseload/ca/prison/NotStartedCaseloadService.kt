package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TimeServedExternalRecordsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalSummaryRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManagerWithoutUser
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.TIME_SERVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.Clock
import java.time.LocalDate

private const val FOUR_WEEKS = 4L

@Service
class NotStartedCaseloadService(
  private val hdcService: HdcService,
  private val clock: Clock,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
  private val licenceRepository: TimeServedExternalRecordsRepository,
) {
  fun findNotStartedCases(
    licences: List<LicenceCaCase>,
    prisonCaseload: Set<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { it.prisonNumber }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisonerNumber)
    }.toList()

    val nomisRecordsToDeliusRecords = pairNomisRecordsWithDelius(prisonersWithoutLicences)
    val cvlRecords = cvlRecordService.getCvlRecords(prisonersWithoutLicences)

    val casesWithoutLicences = buildManagedCaseDto(nomisRecordsToDeliusRecords, cvlRecords)

    val eligibleCases = filterOffendersEligibleForLicence(casesWithoutLicences)

    return createNotStartedLicenceForCase(eligibleCases)
  }

  private fun createNotStartedLicenceForCase(
    cases: List<ManagedCaseDto>,
  ): List<CaCase> {
    val timeServedExternalRecordsFlags = fetchTimeServedExternalRecordFlags(cases)

    return cases.map { case ->
      var licenceStatus = NOT_STARTED
      if (case.cvlRecord.isInHardStopPeriod) {
        licenceStatus = TIMED_OUT
      }
      val timeServedExternalRecord = timeServedExternalRecordsFlags[case.nomisRecord.bookingId?.toLong()]

      CaCase(
        kind = case.cvlRecord.eligibleKind,
        name = "${case.nomisRecord.firstName} ${case.nomisRecord.lastName}".convertToTitleCase(),
        prisonerNumber = case.nomisRecord.prisonerNumber,
        releaseDate = case.cvlRecord.licenceStartDate,
        releaseDateLabel = releaseDateLabelFactory.fromPrisonerSearch(
          case.cvlRecord.licenceStartDate,
          case.nomisRecord,
        ),
        licenceStatus = licenceStatus,
        nomisLegalStatus = case.nomisRecord.legalStatus,
        isInHardStopPeriod = case.cvlRecord.isInHardStopPeriod,
        tabType = Tabs.determineCaViewCasesTab(
          case.cvlRecord.isDueToBeReleasedInTheNextTwoWorkingDays,
          case.cvlRecord.licenceStartDate,
          licenceCaCase = null,
          clock,
        ),
        probationPractitioner = case.probationPractitioner,
        prisonCode = case.nomisRecord.prisonId,
        prisonDescription = case.nomisRecord.prisonName,
        hardStopKind = case.cvlRecord.hardStopKind,
        hasNomisLicence = timeServedExternalRecord != null,
        lastWorkedOnBy = timeServedExternalRecord?.lastWorkedOnBy,
      )
    }
  }

  private fun fetchTimeServedExternalRecordFlags(cases: List<ManagedCaseDto>): Map<Long, TimeServedExternalSummaryRecord> {
    val bookingIds = cases
      .filter { it.cvlRecord.hardStopKind == TIME_SERVED }
      .mapNotNull { it.nomisRecord.bookingId }

    if (bookingIds.isEmpty()) return emptyMap()

    return licenceRepository.getTimeServedExternalSummaryRecords(bookingIds)
      .associate { it.bookingId to it }
  }

  private fun filterOffendersEligibleForLicence(offenders: List<ManagedCaseDto>): List<ManagedCaseDto> {
    val eligibleOffenders = offenders.filter { it.cvlRecord.isEligible }

    if (eligibleOffenders.isEmpty()) return eligibleOffenders

    val hdcStatuses = hdcService.getHdcStatus(eligibleOffenders.map { it.nomisRecord })

    return eligibleOffenders.filter { hdcStatuses.canUnstartedCaseBeSeenByCa(it.nomisRecord.bookingId?.toLong()!!) }
  }

  private fun getPrisonersApproachingRelease(
    prisonCaseload: Set<String>,
    overrideClock: Clock? = null,
  ): Page<PrisonerSearchPrisoner> {
    val now = overrideClock ?: clock
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(FOUR_WEEKS)
    return prisonerSearchApiClient.searchPrisonersByReleaseDate(
      today,
      todayPlusFourWeeks,
      prisonCaseload,
      page = 0,
    )
  }

  private fun pairNomisRecordsWithDelius(nomisRecords: List<PrisonerSearchPrisoner>): List<Pair<PrisonerSearchPrisoner, CommunityManagerWithoutUser>> {
    val caseloadNomisIds = nomisRecords.map { it.prisonerNumber }
    val coms = deliusApiClient.getOffenderManagersWithoutUser(caseloadNomisIds)
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
    nomisRecordsToComRecords: List<Pair<PrisonerSearchPrisoner, CommunityManagerWithoutUser>>,
    cvlRecords: List<CvlRecord>,
  ): List<ManagedCaseDto> = nomisRecordsToComRecords
    .map { (nomisRecord, com) ->
      val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
      ManagedCaseDto(
        nomisRecord = nomisRecord,
        cvlRecord = cvlRecord,
        probationPractitioner = if (com.unallocated) {
          null
        } else {
          ProbationPractitioner(
            staffCode = com.code,
            name = com.name.fullName(),
          )
        },
      )
    }

  private data class ManagedCaseDto(
    val nomisRecord: PrisonerSearchPrisoner,
    val cvlRecord: CvlRecord,
    val probationPractitioner: ProbationPractitioner?,
  )
}
