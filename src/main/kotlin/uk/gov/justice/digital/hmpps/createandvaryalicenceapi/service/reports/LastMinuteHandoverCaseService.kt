package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.reports

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.LastMinuteHandoverCaseResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import java.time.Clock
import java.time.LocalDate

@Service
class LastMinuteHandoverCaseService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val licenceRepository: LicenceRepository,
  private val hdcService: HdcService,
  private val deliusApiClient: DeliusApiClient,
  @param:Value("\${hmpps.cases.last-minute.prisons}")
  private val lastMinutePrisonCodes: Set<String>,
  private val cvlRecordService: CvlRecordService,
  private val clock: Clock,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun getLastMinuteCases(): List<LastMinuteHandoverCaseResponse> {
    log.info("Getting last minute cases")
    val prisoners = getPrisonerData()

    val builder = LastMinuteReportBuilder(
      licenceRepository,
      hdcService,
      deliusApiClient,
      cvlRecordService,
      clock,
    )

    return builder.start(prisoners)
      .enrichWithDeliusData()
      .enrichWithCvlRecords()
      .withEligibleCandidates()
      .withPreSubmissionState()
      .filterOutHdcEligible()
      .filterByLicenceStartDates()
      .build()
  }

  private fun getPrisonerData(): Map<String, PrisonerSearchPrisoner> {
    log.info("Getting prisoner data for $lastMinutePrisonCodes")
    val today = LocalDate.now(clock)
    val nextWeek = today.plusWeeks(1)
    val prisoners = prisonerSearchApiClient.getAllByReleaseDate(today, nextWeek, lastMinutePrisonCodes)
    log.info("Found ${prisoners.size} prisoners")
    return prisoners.associateBy { it.prisonerNumber }
  }
}
