package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient

@Service
class SupportService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val hdcService: HdcService,
  private val eligibilityService: EligibilityService,
  private val iS91DeterminationService: IS91DeterminationService,
  private val deliusApiClient: DeliusApiClient,
) {
  fun getIneligibilityReasons(prisonNumber: String): EligibilityAssessment {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    val prisoner = prisoners.first()
    val deliusRecord = deliusApiClient.getOffenderManagers(listOf(prisoner.prisonerNumber)).first()
    val eligibility = eligibilityService.getEligibilityAssessment(prisoner, deliusRecord.team.provider.code)
    val hdcReasonIfPresent = if (prisoner.isApprovedForHdc()) listOf("Approved for HDC") else emptyList()
    return eligibility.copy(
      genericIneligibilityReasons = eligibility.genericIneligibilityReasons + hdcReasonIfPresent,
      isEligible = eligibility.isEligible && hdcReasonIfPresent.isEmpty(),
      eligibleKind = if (hdcReasonIfPresent.isEmpty()) eligibility.eligibleKind else null,
      ineligibilityReasons = eligibility.ineligibilityReasons + hdcReasonIfPresent,
    )
  }

  fun getIS91Status(prisonNumber: String): Boolean {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    return iS91DeterminationService.isIS91Case(prisoners.first())
  }

  private fun PrisonerSearchPrisoner.isApprovedForHdc() = hdcService.isApprovedForHdc(this.bookingId?.toLong()!!, this.homeDetentionCurfewEligibilityDate)
}
