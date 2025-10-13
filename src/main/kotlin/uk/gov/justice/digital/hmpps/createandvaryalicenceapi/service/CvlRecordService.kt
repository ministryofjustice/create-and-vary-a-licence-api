package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Service
class CvlRecordService(
  private val eligibilityService: EligibilityService,
  private val releaseDateService: ReleaseDateService,
) {

  fun getCvlRecord(prisoner: PrisonerSearchPrisoner, areaCode: String): CvlRecord {
    val recordList = getCvlRecords(listOf(prisoner), mapOf(prisoner.prisonerNumber to areaCode))
    return recordList.first { it.nomisId == prisoner.prisonerNumber }
  }

  fun getCvlRecords(prisoners: List<PrisonerSearchPrisoner>, nomisIdsToAreaCodes: Map<String, String>): List<CvlRecord> {
    val nomisIdsToEligibility = eligibilityService.getEligibilityAssessments(prisoners, nomisIdsToAreaCodes)
    val nomisIdsToEligibleKinds = nomisIdsToEligibility.map { (nomisId, eligibility) -> nomisId to eligibility.eligibleKind }.toMap()
    val nomisIdsToLicenceStartDates = releaseDateService.getLicenceStartDates(prisoners, nomisIdsToEligibleKinds)
    return prisoners.map { prisoner ->
      val eligibility = nomisIdsToEligibility[prisoner.prisonerNumber]!!

      CvlRecord(
        nomisId = prisoner.prisonerNumber,
        licenceStartDate = nomisIdsToLicenceStartDates[prisoner.prisonerNumber],
        isEligible = eligibility.isEligible,
        eligibleKind = eligibility.eligibleKind,
        ineligiblityReasons = eligibility.ineligiblityReasons,
      )
    }
  }
}
