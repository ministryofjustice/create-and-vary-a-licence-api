package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TimeServedExternalRecordsRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.TimeServedExternalSummaryRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
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
  private val clock: Clock,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
  private val licenceRepository: TimeServedExternalRecordsRepository,
  @param:Value("\${timeserved.max.days.crd.before.today:28}") private val maxNumberOfDaysBeforeTodayForCrdTimeserved: Long = 28,
) {
  fun findNotStartedCases(
    licences: List<LicenceCaCase>,
    prisonCaseload: Set<String>,
  ): List<CaCase> {
    val licenceNomisIds = licences.map { it.prisonNumber }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisonerNumber)
    }

    val casesWithoutLicences = buildManagedCaseDto(prisonersWithoutLicences)
    val eligibleCases = casesWithoutLicences.filter { it.cvlRecord.isEligible }

    return createNotStartedLicenceForCase(eligibleCases)
  }

  private fun getPrisonersApproachingRelease(
    prisonCaseload: Set<String>,
    overrideClock: Clock? = null,
  ): List<PrisonerSearchPrisoner> {
    val now = overrideClock ?: clock
    val today = LocalDate.now(now)
    val todayPlusFourWeeks = LocalDate.now(now).plusWeeks(FOUR_WEEKS)
    return prisonerSearchApiClient.searchPrisonersByReleaseDate(
      today.minusDays(maxNumberOfDaysBeforeTodayForCrdTimeserved - 1),
      todayPlusFourWeeks,
      prisonCaseload,
      page = 0,
    ).toList()
  }

  private fun buildManagedCaseDto(prisoners: List<PrisonerSearchPrisoner>): List<ManagedCaseDto> {
    val cvlRecords = cvlRecordService.getCvlRecords(prisoners).associateBy { it.nomisId }
    return prisoners.map {
      ManagedCaseDto(
        nomisRecord = it,
        cvlRecord = requireNotNull(cvlRecords[it.prisonerNumber]) { it.prisonerNumber },
      )
    }
  }

  private fun createNotStartedLicenceForCase(
    cases: List<ManagedCaseDto>,
  ): List<CaCase> {
    val timeServedExternalRecordsFlags = fetchTimeServedExternalRecordFlags(cases)
    val probationPractitioners = getProbationPractitioners(cases.map { it.nomisRecord.prisonerNumber })

    return cases.map { case ->
      var licenceStatus = NOT_STARTED
      if (case.cvlRecord.isTimedOut) {
        licenceStatus = TIMED_OUT
      }
      val timeServedExternalRecord = timeServedExternalRecordsFlags[case.nomisRecord.bookingId?.toLong()]
      val kind = case.cvlRecord.hardStopKind ?: case.cvlRecord.eligibleKind

      CaCase(
        kind = kind,
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
          timeServedCase = case.cvlRecord.hardStopKind == TIME_SERVED,
          clock,
        ),
        probationPractitioner = probationPractitioners[case.nomisRecord.prisonerNumber]
          ?: ProbationPractitioner.UNALLOCATED,
        prisonCode = case.nomisRecord.prisonId,
        prisonDescription = case.nomisRecord.prisonName,
        hasNomisLicence = timeServedExternalRecord != null,
        lastWorkedOnBy = timeServedExternalRecord?.lastWorkedOnBy,
      )
    }
  }

  private fun fetchTimeServedExternalRecordFlags(cases: List<ManagedCaseDto>): Map<Long, TimeServedExternalSummaryRecord> {
    val bookingIds = cases.filter { it.cvlRecord.hardStopKind == TIME_SERVED }.mapNotNull { it.nomisRecord.bookingId }

    if (bookingIds.isEmpty()) return emptyMap()

    return licenceRepository.getTimeServedExternalSummaryRecords(bookingIds).associateBy { it.bookingId }
  }

  private fun getProbationPractitioners(prisonNumbers: List<String>): Map<String, ProbationPractitioner> {
    val coms = deliusApiClient.getOffenderManagersWithoutUser(prisonNumbers)
    return coms.mapNotNull {
      if (it.unallocated) {
        it.case.nomisId!! to ProbationPractitioner.unallocated(it.code)
      } else {
        it.case.nomisId!! to ProbationPractitioner(
          staffCode = it.code,
          name = it.name.fullName(),
          allocated = true,
        )
      }
    }.toMap()
  }

  private data class ManagedCaseDto(
    val nomisRecord: PrisonerSearchPrisoner,
    val cvlRecord: CvlRecord,
  )
}
