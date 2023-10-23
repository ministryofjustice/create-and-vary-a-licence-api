package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient

@Service
class IS91DeterminationService(
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
) {

  private companion object IS91Constants {
    const val OFFENCE_DESCRIPTION = "ILLEGAL IMMIGRANT/DETAINEE"
    val resultCodes = setOf("5500", "4022")
  }

  fun getIS91AndExtraditionBookingIds(bookingIds: List<Long>): List<Long> {
    val prisoners = prisonerSearchApiClient.searchPrisonersByBookingIds(bookingIds)
    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == OFFENCE_DESCRIPTION }
    val immigrationDetaineeBookings = immigrationDetainees.map { it.bookingId.toLong() }
    val is91OutcomeBookings = bookingsWithIS91Outcomes(nonImmigrationDetainees.map { it.bookingId.toLong() })
    return immigrationDetaineeBookings.plus(is91OutcomeBookings)
  }

  private fun bookingsWithIS91Outcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds)
    return courtEventOutcomes.filter { resultCodes.contains(it.outcomeReasonCode) }.map { it.bookingId }
  }
}
