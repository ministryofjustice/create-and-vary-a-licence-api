package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.toSentenceDateHolder

@Service
class CvlRecordService(
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getCvlRecord(prisoner: PrisonerSearchPrisoner, areaCode: String): CvlRecord {
    val recordList = getCvlRecords(listOf(prisoner), mapOf(prisoner.prisonerNumber to areaCode))
    return recordList.first { it.nomisId == prisoner.prisonerNumber }
  }

  fun getCvlRecords(
    prisoners: List<PrisonerSearchPrisoner>,
    nomisIdsToAreaCodes: Map<String, String>,
  ): List<CvlRecord> {
    val nomisIdsToEligibility = eligibilityService.getEligibilityAssessments(prisoners, nomisIdsToAreaCodes)
    val nomisIdsToEligibleKinds =
      nomisIdsToEligibility.map { (nomisId, eligibility) -> nomisId to eligibility.eligibleKind }.toMap()
    val nomisIdsToLicenceStartDates = releaseDateService.getLicenceStartDates(prisoners, nomisIdsToEligibleKinds)
    return prisoners.map { prisoner ->
      val eligibility = nomisIdsToEligibility[prisoner.prisonerNumber]!!
      val licenceStartDate = nomisIdsToLicenceStartDates[prisoner.prisonerNumber]
      val hardStopKind = releaseDateService.getHardStopKind(prisoner.toSentenceDateHolder(licenceStartDate))

      CvlRecord(
        nomisId = prisoner.prisonerNumber,
        licenceStartDate = licenceStartDate,
        isEligible = eligibility.isEligible,
        eligibleKind = eligibility.eligibleKind,
        ineligibilityReasons = eligibility.ineligibilityReasons,
        hardStopKind = hardStopKind,
        isInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceStartDate),
        hardStopWarningDate = releaseDateService.getHardStopWarningDate(licenceStartDate),
        hardStopDate = releaseDateService.getHardStopDate(licenceStartDate),
        isEligibleForEarlyRelease = releaseDateService.isEligibleForEarlyRelease(
          prisoner.toSentenceDateHolder(
            licenceStartDate,
          ),
        ),
        isDueToBeReleasedInTheNextTwoWorkingDays = releaseDateService.isDueToBeReleasedInTheNextTwoWorkingDays(
          licenceStartDate,
        ),
      )
    }
  }
}
