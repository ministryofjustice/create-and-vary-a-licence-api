package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Service
class SupportService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val eligibilityService: EligibilityService,
  private val iS91DeterminationService: IS91DeterminationService,
) {
  fun getIneligibilityReasons(prisonNumber: String): List<String> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    val reasons = eligibilityService.getIneligibilityReasons(prisoners.first())
    val hdcReasonIfPresent = if (prisoners.findBookingsWithHdc().isEmpty()) emptyList() else listOf("Approved for HDC")
    return reasons + hdcReasonIfPresent
  }

  fun getIS91Status(prisonNumber: String): Boolean {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    return iS91DeterminationService.isIS91Case(prisoners.first())
  }

  private fun List<PrisonerSearchPrisoner>.findBookingsWithHdc(): List<Long> {
    val bookingsWithHdc = this
      .filter { it.homeDetentionCurfewEligibilityDate != null }
      .mapNotNull { it.bookingId?.toLong() }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return hdcStatuses.filter { it.approvalStatus == "APPROVED" }.mapNotNull { it.bookingId }
  }
}
