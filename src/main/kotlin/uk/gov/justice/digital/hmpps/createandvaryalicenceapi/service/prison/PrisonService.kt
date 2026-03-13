package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PrisonService(val prisonApiClient: PrisonApiClient, val prisonerSearchApi: PrisonerSearchApiClient) {
  fun getPrisonerDetail(nomisId: String): PrisonApiPrisoner = prisonApiClient.getPrisonerDetail(nomisId)

  fun searchPrisonersByBookingIds(bookingIds: List<Long>): List<PrisonerSearchPrisoner> = prisonerSearchApi.searchPrisonersByBookingIds(bookingIds)

  fun getPrisonerLatestSentenceStartDate(bookingId: Long): LocalDate? {
    val sentencesAndOffences = prisonApiClient.getPrisonerSentenceAndOffences(bookingId)
    val sentenceStartDates = sentencesAndOffences.mapNotNull { it.sentenceDate }
    return sentenceStartDates.max()
  }
}
