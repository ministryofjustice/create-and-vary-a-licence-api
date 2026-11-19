package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remandAndSentencing.RemandAndSentencingApiClient

@Service
class RemandDeterminationService(
  private val remandAndSentencingApiClient: RemandAndSentencingApiClient,
) {
  fun getRemandBookingIds(prisoners: List<PrisonerSearchPrisoner>): List<Long> {
    if (prisoners.isEmpty()) return emptyList()

    val bookingIdsByPrisonerNumber = prisoners.mapNotNull { prisoner ->
      val bookingId = prisoner.bookingId?.toLong() ?: return@mapNotNull null
      prisoner.prisonerNumber to bookingId
    }.toMap()

    val personStatuses = remandAndSentencingApiClient.getPersonStatuses(prisoners.map { it.prisonerNumber })
      ?: return emptyList()

    return personStatuses.peopleStatus
      .filterValues { it.isOnRemand }
      .mapNotNull { (prisonerNumber, _) -> bookingIdsByPrisonerNumber[prisonerNumber] }
  }
}
