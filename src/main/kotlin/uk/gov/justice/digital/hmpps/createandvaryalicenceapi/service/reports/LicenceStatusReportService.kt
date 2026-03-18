package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LicenceStatusResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecord
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.LocalDate

@Service
class LicenceStatusReportService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val deliusApiClient: DeliusApiClient,
  private val cvlRecordService: CvlRecordService,
  private val clock: Clock,
  private val licenceRepository: LicenceRepository,
  private val releaseDateService: ReleaseDateService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCases(): List<LicenceStatusResponse> {
    val today = LocalDate.now(clock)
    log.info("Getting cases for licence status report for $today")

    val licences = licenceRepository.findAllPreReleaseAndActiveLicencesForToday().associateBy { it.nomsId!! }
    log.info("Found ${licences.size} pre release and active licences with a licence start date of today")

    val nextWeek = today.plusWeeks(1)
    val nomisRecords = getPrisonerData(today, nextWeek)
    log.info("Found ${nomisRecords.size} prisoners with a release date range of between $today and $nextWeek")

    val nomisRecordswithDeliusData = enrichWithDeliusData(nomisRecords)
    log.info("Found ${nomisRecordswithDeliusData.size} prisoners with delius records")

    val cvlRecords = cvlRecordService.getCvlRecords(nomisRecordswithDeliusData.keys.toList())

    val eligibleCases = findEligibleCases(nomisRecordswithDeliusData, cvlRecords, licences)
    log.info("Out of {} prisoners, found {} eligible cases with a LSD of today", nomisRecordswithDeliusData.size, eligibleCases.size)

    log.info("Total of ${licences.size + eligibleCases.size } cases with a licence start date of today")

    val notStartedCases = eligibleCases.map { (prisoner, com) ->
      LicenceStatusResponse(
        probationRegion = com.team.borough.description,
        prison = prisoner.prisonName,
        crn = com.case.crn,
        nomisNumber = prisoner.prisonerNumber,
        prisonerName = prisoner.fullName(),
        // All eligible not started cases would now be TIMED_OUT as the report is checking all licence start dates for today
        status = LicenceStatus.TIMED_OUT,
      )
    }

    val casesWithLicence = licences.values.map { licence ->
      LicenceStatusResponse(
        probationRegion = licence.probationPduDescription,
        prison = licence.prisonDescription,
        crn = licence.crn,
        nomisNumber = licence.nomsId,
        prisonerName = licence.let { "${it.forename} ${it.middleNames} ${it.surname}" },
        status = licence.statusCode,
      )
    }

    return notStartedCases + casesWithLicence
  }

  private fun getPrisonerData(fromDate: LocalDate, toDate: LocalDate): List<PrisonerSearchPrisoner> = prisonerSearchApiClient.getAllByReleaseDate(fromDate, toDate)

  private fun enrichWithDeliusData(candidates: List<PrisonerSearchPrisoner>): Map<PrisonerSearchPrisoner, CommunityManager> {
    val coms = deliusApiClient.getOffenderManagers(candidates.map {it.prisonerNumber}).filter { it.case.nomisId != null }.associateBy { it.case.nomisId!!}
    return candidates.mapNotNull {
      val com = coms[it.prisonerNumber] ?: return@mapNotNull null
      it to com
    }.toMap()
  }

  private fun findEligibleCases(
    nomisRecordswithDeliusData: Map<PrisonerSearchPrisoner, CommunityManager>,
    cvlRecords: List<CvlRecord>,
    licences: Map<String, Licence>
  ): Map<PrisonerSearchPrisoner, CommunityManager> {
    val nomisIdsWithALicence = licences.keys
    return nomisRecordswithDeliusData.filter { (nomisRecord, _) ->
      val cvlRecord = cvlRecords.first { cvlRecord -> cvlRecord.nomisId == nomisRecord.prisonerNumber }
      val potentialLsd = releaseDateService.getLicenceStartDate(nomisRecord, cvlRecord.eligibleKind)
      return@filter cvlRecord.isEligible && potentialLsd == LocalDate.now(clock) && nomisRecord.prisonerNumber !in nomisIdsWithALicence
    }
  }
}
