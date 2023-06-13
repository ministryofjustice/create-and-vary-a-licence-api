package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient

@Service
class IS91DeterminationService(
  private val prisonApiClient: PrisonApiClient
) {

  private object IS91Constants {
    val resultCodes = setOf("5500", "4022", "3006", "5502")
    const val offenceCode = "IA99000-001N"
  }

  fun getIS91AndExtraditionBookingIds(bookingIds: List<Long>): List<Long> {
    val offenceHistories = prisonApiClient.getOffenceHistories(bookingIds)
    val is91AndExtraditionOffenceHistories = offenceHistories.filter {
      IS91Constants.resultCodes.contains(it.primaryResultCode) ||
        IS91Constants.resultCodes.contains(it.secondaryResultCode) ||
        it.offenceCode == IS91Constants.offenceCode
    }
    return is91AndExtraditionOffenceHistories.map { it.bookingId }
  }
}
