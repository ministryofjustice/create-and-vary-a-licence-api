package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

@Service
class SupportService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val eligibilityService: EligibilityService,
  private val iS91DeterminationService: IS91DeterminationService,
  private val hdcService: HdcService,
) {
  fun getIneligibilityReasons(prisonNumber: String): EligibilityAssessment {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    val prisoner = prisoners.first()
    val hdcStatus = hdcService.getHdcStatus(listOf(prisoner))
    return eligibilityService.getEligibilityAssessment(prisoner, hdcStatus)
  }

  fun getIS91Status(prisonNumber: String): Boolean {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    return iS91DeterminationService.isIS91Case(prisoners.first())
  }
}
