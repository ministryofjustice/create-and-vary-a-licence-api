package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.PrisonerWithCvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class CaseloadService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val releaseDateService: ReleaseDateService,
  private val cvlRecordService: CvlRecordService,
  private val deliusApiClient: DeliusApiClient,
) {

  fun getPrisonersByNumber(nomisIds: List<String>): List<PrisonerSearchPrisoner> = prisonerSearchApiClient.searchPrisonersByNomisIds(nomisIds)

  fun getPrisoner(nomisId: String): PrisonerWithCvlFields {
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomisId)).firstOrNull() ?: throw EntityNotFoundException(nomisId)
    val comRecord = deliusApiClient.getOffenderManager(nomisId)!!
    val cvlRecord = cvlRecordService.getCvlRecord(prisoner, comRecord.team.provider.code)
    val sentenceDateHolder = prisoner.toSentenceDateHolder(cvlRecord.licenceStartDate)

    return PrisonerWithCvlFields(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(prisoner),
        hardStopDate = releaseDateService.getHardStopDate(cvlRecord.licenceStartDate),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(cvlRecord.licenceStartDate),
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(cvlRecord.licenceStartDate),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(sentenceDateHolder),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        licenceStartDate = cvlRecord.licenceStartDate,
        licenceKind = cvlRecord.eligibleKind,
      ),
    )
  }
}
