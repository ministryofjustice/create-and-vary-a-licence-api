package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.InvalidStateException
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.corePersonRecord.CorePersonRecordApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase

@Service
class CaseService(
  private val corePersonRecordApiClient: CorePersonRecordApiClient,
  private val cvlRecordService: CvlRecordService,
  private val deliusApiClient: DeliusApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {
  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getPrisoner(nomisId: String): PrisonerWithCvlFields {
    val prisoner =
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(
        nomisId,
      )
    val cvlRecord = cvlRecordService.getCvlRecord(prisoner)

    return PrisonerWithCvlFields(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = cvlRecord.licenceType,
        hardStopDate = cvlRecord.hardStopDate,
        hardStopWarningDate = cvlRecord.hardStopWarningDate,
        isInHardStopPeriod = cvlRecord.isInHardStopPeriod,
        isEligibleForEarlyRelease = cvlRecord.isEligibleForEarlyRelease,
        isDueToBeReleasedInTheNextTwoWorkingDays = cvlRecord.isDueToBeReleasedInTheNextTwoWorkingDays,
        licenceStartDate = cvlRecord.licenceStartDate,
        licenceKind = cvlRecord.eligibleKind,
        hardStopKind = cvlRecord.hardStopKind,
      ),
    )
  }

  fun getProbationCase(prisonNumber: String): ProbationCase {
    val deliusRecord = deliusApiClient.getProbationCase(prisonNumber)
    if (deliusRecord == null) {
      logCorePersonRecord(prisonNumber)
      throw InvalidStateException("Could not find a delius record for nomis id: $prisonNumber")
    }
    return ProbationCase(
      crn = deliusRecord.crn,
      nomisId = deliusRecord.nomisId,
      croNumber = deliusRecord.croNumber,
      pncNumber = deliusRecord.pncNumber,
    )
  }

  private fun logCorePersonRecord(prisonNumber: String) {
    val corePersonRecord = corePersonRecordApiClient.getPersonRecord(prisonNumber)
    val crns = corePersonRecord.identifiers.crns
    val baseMessage = "Could not find a delius record for nomis id: $prisonNumber"
    if (crns.isEmpty()) {
      log.info("$baseMessage - no crns found in core person record")
    } else if (crns.size == 1) {
      log.info("$baseMessage - one crn found in core person record: ${crns.first()}")
    } else {
      log.info("$baseMessage - ${crns.size} crns found in core person record: ${crns.joinToString()}")
    }
  }
}
