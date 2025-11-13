package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

@Service
class CaseService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val cvlRecordService: CvlRecordService,
) {

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
      ),
    )
  }
}
