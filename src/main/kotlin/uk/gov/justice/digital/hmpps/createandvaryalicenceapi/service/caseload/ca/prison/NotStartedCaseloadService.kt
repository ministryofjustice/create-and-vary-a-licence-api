package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison

import org.springframework.data.domain.Page
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.model.LicenceCaCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.DeliusRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCaseCvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ManagedCaseDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions.convertToTitleCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ManagedOffenderCrn
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.fullName
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.util.ReleaseDateLabelFactory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.NOT_STARTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.Clock
import java.time.LocalDate

@Service
class NotStartedCaseloadService(
  private val hdcService: HdcService,
  private val clock: Clock,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
  private val releaseDateLabelFactory: ReleaseDateLabelFactory,
  private val cvlRecordService: CvlRecordService,
) {
  fun findAndFormatNotStartedCases(
    licenceCaCases: List<LicenceCaCase>,
    prisonCaseload: Set<String>,
  ): List<CaCase> {
    val licenceNomisIds = licenceCaCases.map { it.prisonNumber }
    val prisonersApproachingRelease = getPrisonersApproachingRelease(prisonCaseload)

    val prisonersWithoutLicences = prisonersApproachingRelease.filter { p ->
      !licenceNomisIds.contains(p.prisonerNumber)
    }.toList()

    val nomisRecordsToDeliusRecords = pairNomisRecordsWithDelius(prisonersWithoutLicences)
    val nomisIdsToAreaCodes =
      nomisRecordsToDeliusRecords.associate { (nomisRecord, deliusRecord) -> nomisRecord.prisonerNumber to deliusRecord.team.provider.code }

    val cvlRecords = cvlRecordService.getCvlRecords(prisonersWithoutLicences, nomisIdsToAreaCodes)

    val casesWithoutLicences = buildManagedCaseDto(nomisRecordsToDeliusRecords, cvlRecords)

    val eligibleCases = filterOffendersEligibleForLicence(casesWithoutLicences)

    return createNotStartedLicenceForCase(eligibleCases)
  }

  private fun createNotStartedLicenceForCase(
    cases: List<ManagedCaseDto>,
  ): List<CaCase> = cases.map { case ->
    val sentenceDateHolder = case.nomisRecord.toSentenceDateHolder(case.cvlRecord.licenceStartDate)

    // Default status (if not overridden below) will show the case as clickable on case lists
    var licenceStatus = NOT_STARTED

    if (releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate)) {
      licenceStatus = TIMED_OUT
    }

    val com = case.deliusRecord.managedOffenderCrn.staff

    CaCase(
      kind = case.cvlRecord.eligibleKind,
      name = case.nomisRecord.let { "${it.firstName} ${it.lastName}".convertToTitleCase() },
      prisonerNumber = case.nomisRecord.prisonerNumber,
      releaseDate = case.cvlRecord.licenceStartDate,
      releaseDateLabel = releaseDateLabelFactory.fromPrisonerSearch(case.cvlRecord.licenceStartDate, case.nomisRecord),
      licenceStatus = licenceStatus,
      nomisLegalStatus = case.nomisRecord.legalStatus,
      isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate),
      tabType = Tabs.determineCaViewCasesTab(
        releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        case.cvlRecord.licenceStartDate,
        licenceCaCase = null,
        clock,
      ),
      probationPractitioner = ProbationPractitioner(
        staffCode = com?.code,
        name = com?.name?.fullName(),
      ),
      prisonCode = case.nomisRecord.prisonId,
      prisonDescription = case.nomisRecord.prisonName,
    )
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

  private fun pairNomisRecordsWithDelius(nomisRecords: List<PrisonerSearchPrisoner>): List<Pair<PrisonerSearchPrisoner, CommunityManager>> {
    val caseloadNomisIds = nomisRecords.map { it.prisonerNumber }
    val coms = deliusApiClient.getOffenderManagers(caseloadNomisIds)
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
    nomisRecordsToComRecords: List<Pair<PrisonerSearchPrisoner, CommunityManager>>,
    cvlRecords: List<CvlRecord>,
  ): List<ManagedCaseDto> = nomisRecordsToComRecords
    .map { (nomisRecord, com) ->
      val cvlRecord = cvlRecords.first { it.nomisId == nomisRecord.prisonerNumber }
      ManagedCaseDto(
        nomisRecord = nomisRecord,
        cvlRecord = ManagedCaseCvlRecord(
          isEligible = cvlRecord.isEligible,
          eligibleKind = cvlRecord.eligibleKind,
          licenceStartDate = cvlRecord.licenceStartDate,
        ),
        deliusRecord = DeliusRecord(
          com.case,
          ManagedOffenderCrn(
            staff = StaffDetail(
              code = com.code,
              name = com.name,
              unallocated = com.unallocated,
            ),
            team = com.team,
          ),
        ),
      )
    }
}
