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
    const val DEPORTATION_RECOMMENDED = "3006"
    const val IMMIGRATION_DETAINEE = "5500"
    const val IMMIGRATION_DECISION_TO_DEPORT = "5502"
    const val EXTRADITED = "4022"

    val IS91_RESULT_CODES =
      listOf(DEPORTATION_RECOMMENDED, EXTRADITED, IMMIGRATION_DETAINEE, IMMIGRATION_DECISION_TO_DEPORT)
  }

  fun isIS91Case(prisoner: PrisonerSearchPrisoner) = getIS91AndExtraditionBookingIds(listOf(prisoner)).isNotEmpty()

  fun getIS91AndExtraditionBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == OFFENCE_DESCRIPTION }
    val immigrationDetaineeBookings = immigrationDetainees.mapNotNull { it.bookingId?.toLong() }
    val is91OutcomeBookings = bookingsWithIS91Outcomes(nonImmigrationDetainees.mapNotNull { it.bookingId?.toLong() })
    return immigrationDetaineeBookings + is91OutcomeBookings
  }

  private fun bookingsWithIS91Outcomes(bookingIds: List<Long>): List<Long> {
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(bookingIds, IS91_RESULT_CODES)
    return courtEventOutcomes.map { it.bookingId }
  }
}
