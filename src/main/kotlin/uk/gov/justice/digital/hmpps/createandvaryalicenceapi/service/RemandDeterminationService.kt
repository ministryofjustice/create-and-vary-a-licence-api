package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents

@Service
class RemandDeterminationService(
  private val prisonApiClient: PrisonApiClient,
) {

  fun isRemandCase(prisoner: PrisonerSearchPrisoner) = getRemandBookingIds(listOf(prisoner)).isNotEmpty()

  fun getRemandBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    val potentialRemandBookings = prisoners.mapNotNull { it.bookingId?.toLong() }
    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(potentialRemandBookings, RemandCourtEvents.getRemandCourtCodes())
    return courtEventOutcomes.map { it.bookingId }
  }
}
