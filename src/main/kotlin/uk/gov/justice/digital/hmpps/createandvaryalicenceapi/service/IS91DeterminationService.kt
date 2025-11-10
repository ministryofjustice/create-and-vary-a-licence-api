package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Service
class IS91DeterminationService(
  private val prisonApiClient: PrisonApiClient,
) {

  private companion object IS91Constants {
    const val OFFENCE_DESCRIPTION = "ILLEGAL IMMIGRANT/DETAINEE"
    val RESULT_CODES = listOf("3006", "4022", "5500", "5502")
  }

  fun isIS91Case(prisoner: PrisonerSearchPrisoner) = getIS91AndExtraditionBookingIds(listOf(prisoner)).isNotEmpty()

  fun getIS91AndExtraditionBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == OFFENCE_DESCRIPTION }
    val immigrationDetaineeBookings = immigrationDetainees.mapNotNull { it.bookingId?.toLong() }
    val is91OutcomeBookings = bookingsWithIS91Outcomes(nonImmigrationDetainees.mapNotNull { it.bookingId?.toLong() })
    return immigrationDetaineeBookings + is91OutcomeBookings
  }

  private fun bookingsWithIS91Outcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds, RESULT_CODES)
    return courtEventOutcomes.map { it.bookingId }
  }
}
