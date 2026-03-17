package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LicenceStatusResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
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
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getCases(): List<LicenceStatusResponse> {
    val nomisRecords = getPrisonerData()
    val nomisIds = nomisRecords.keys.toList()

    val deliusRecords =
      deliusApiClient.getOffenderManagers(nomisIds).filter { it.case.nomisId != null }.associateBy { it.case.nomisId!! }
    log.info("Found ${deliusRecords.size} delius records")

    val cvlRecords = cvlRecordService.getCvlRecords(nomisRecords.values.toList())

    val licences = licenceRepository.findAllPreReleaseAndActiveLicencesForToday().associateBy { it.nomsId!! }

    val eligibleNotStartedCases = nomisRecords
      .filter { (nomsId, _) ->
        cvlRecords.any { it.nomisId == nomsId && it.isEligible } && nomsId !in licences
      }
      .map { (nomisId, prisoner) ->
        val deliusRecord = deliusRecords[nomisId]
        LicenceStatusResponse(
          probationRegion = deliusRecord?.team?.borough?.description,
          prison = prisoner.prisonName,
          crn = deliusRecord?.case?.crn,
          nomisNumber = nomisId,
          prisonerName = prisoner.fullName(),
          //All eligible not started cases would now be TIMED_OUT as the report is checking all releases for today
          status = LicenceStatus.TIMED_OUT,
        )
      }
    log.info("Found ${eligibleNotStartedCases.size} eligible cases")

    val relevantLicences = licences.values.map { licence ->
      LicenceStatusResponse(
        probationRegion = licence.probationPduDescription,
        prison = licence.prisonDescription,
        crn = licence.crn,
        nomisNumber = licence.nomsId,
        prisonerName = licence.let { "${it.forename} ${it.middleNames} ${it.surname}" },
        status = licence.statusCode,
      )
    }
    log.info("Found ${relevantLicences.size} licences")

    return eligibleNotStartedCases + relevantLicences
  }

  private fun getPrisonerData(): Map<String, PrisonerSearchPrisoner> {
    val today = LocalDate.now(clock)
    log.info("Gathering prisoners with release dates on $today")
    val candidates = prisonerSearchApiClient.getAllByReleaseDate(today, today)
    log.info("Found ${candidates.size} prisoners")
    return candidates.associateBy { it.prisonerNumber }
  }
}
