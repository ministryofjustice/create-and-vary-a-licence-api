package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remand

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Service
class RemandIdentificationService(
  private val prisonApiClient: PrisonApiClient,
) {

  fun isRemandBooking(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.bookingId?.let { bookingId ->
    bookingsWithRemandOutcomes(listOf(bookingId.toLong())).isNotEmpty()
  } ?: false

  private fun bookingsWithRemandOutcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds, RemandCourtEvents.REMAND_CODES)
    return courtEventOutcomes.map { it.bookingId }
  }
}
