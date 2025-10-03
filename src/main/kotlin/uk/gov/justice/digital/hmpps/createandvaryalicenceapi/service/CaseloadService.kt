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
    val licenceStartDate = cvlRecordService.getCvlRecord(prisoner, comRecord.team.provider.code).licenceStartDate
    val sentenceDateHolder = prisoner.toSentenceDateHolder(licenceStartDate)

    return PrisonerWithCvlFields(
      prisoner = prisoner.toPrisoner(),
      cvl = CvlFields(
        licenceType = LicenceType.getLicenceType(prisoner),
        hardStopDate = releaseDateService.getHardStopDate(sentenceDateHolder.licenceStartDate),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(sentenceDateHolder.licenceStartDate),
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(sentenceDateHolder.licenceStartDate),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(sentenceDateHolder),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          sentenceDateHolder,
        ),
        licenceStartDate = licenceStartDate,
      ),
    )
  }
}
