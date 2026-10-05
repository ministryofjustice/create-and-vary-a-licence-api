package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

@Service
class IS91DeterminationService(
  private val prisonApiClient: PrisonApiClient,
) {

  companion object IS91Constants {
    const val IMMIGRATION_OFFENCE = "ILLEGAL IMMIGRANT/DETAINEE"
  }

  enum class Is91CourtEvents(val code: String, val description: String) {
    DEPORTATION_RECOMMENDED("3006", "Deportation recommended"),
    IMMIGRATION_DETAINEE("5500", "Immigration detainee"),
    IMMIGRATION_DECISION_TO_DEPORT("5502", "Immigration decision to deport"),
    EXTRADITED("4022", "Extradited"),
    ;

    companion object {
      fun getCodes() = entries.map { it.code }
      fun getCourtEventDescriptionByCode(code: String) = entries.firstOrNull { it.code == code }?.description
    }
  }

  fun getIS91AndExtraditionBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == IMMIGRATION_OFFENCE }
    val immigrationDetaineeBookings = immigrationDetainees.mapNotNull { it.bookingId?.toLong() }
    val is91OutcomeBookings = bookingsWithIS91Outcomes(nonImmigrationDetainees.mapNotNull { it.bookingId?.toLong() })
    return immigrationDetaineeBookings + is91OutcomeBookings
  }

  private fun bookingsWithIS91Outcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds, Is91CourtEvents.getCodes())
    return courtEventOutcomes.map { it.bookingId }
  }
}
