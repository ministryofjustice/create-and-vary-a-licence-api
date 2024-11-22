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
    val resultCodes = setOf("3006", "4022", "5500", "5502")
  }

  fun isIS91Case(prisoner: PrisonerSearchPrisoner): Boolean {
    if(prisoner.mostSeriousOffence == OFFENCE_DESCRIPTION){
      return true
    }
    return isBookingWithIS91Outcome(prisoner.bookingId?.toLong())
  }

  fun getIS91AndExtraditionBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == OFFENCE_DESCRIPTION }
    val immigrationDetaineeBookings = immigrationDetainees.mapNotNull { it.bookingId?.toLong() }
    val is91OutcomeBookings = bookingsWithIS91Outcomes(nonImmigrationDetainees.mapNotNull { it.bookingId?.toLong() })
    return immigrationDetaineeBookings + is91OutcomeBookings
  }

  private fun bookingsWithIS91Outcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds)
    return courtEventOutcomes.filter { resultCodes.contains(it.outcomeReasonCode) }.map { it.bookingId }
  }

  private fun isBookingWithIS91Outcome(bookingId: Long?): Boolean {
    if(bookingId == null){
      return false
    }
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(listOf(bookingId))
    return courtEventOutcomes.any { resultCodes.contains(it.outcomeReasonCode) }
  }
}
