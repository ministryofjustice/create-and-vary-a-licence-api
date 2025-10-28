package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class CaseService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val cvlRecordService: CvlRecordService,
  private val deliusApiClient: DeliusApiClient,
) {

  fun getPrisoner(nomisId: String): PrisonerWithCvlFields {
    val prisoner =
      prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(
        nomisId,
      )
    val comRecord = deliusApiClient.getOffenderManager(nomisId)!!
    val cvlRecord = cvlRecordService.getCvlRecord(prisoner, comRecord.team.provider.code)

    return PrisonerWithCvlFields(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(prisoner),
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
