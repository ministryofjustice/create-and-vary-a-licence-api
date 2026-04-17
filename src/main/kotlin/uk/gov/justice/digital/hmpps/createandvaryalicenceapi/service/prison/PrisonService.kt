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
    return if (sentenceStartDates.isEmpty()) null else sentenceStartDates.max()
  }

  fun searchPrisonersByNomisIds(nomisIds: List<String>) = prisonerSearchApi.searchPrisonersByNomisIds(nomisIds)

  fun getSentenceAndRecallTypes(bookingId: Long): BookingSentenceAndRecallTypes {
    val bookingsSentenceAndRecallTypes = prisonApiClient.getSentenceAndRecallTypes(listOf(bookingId))
    return bookingsSentenceAndRecallTypes.first()
  }

  fun getRecallType(bookingId: Long): RecallType {
    val sentenceAndRecallTypes = getSentenceAndRecallTypes(bookingId)
    return if (sentenceAndRecallTypes.sentenceTypeRecallTypes.any { it.recallType.isStandardRecall }) {
      RecallType.STANDARD
    } else if (sentenceAndRecallTypes.sentenceTypeRecallTypes.any { it.recallType.isFixedTermRecall }) {
      RecallType.FIXED_TERM
    } else {
      RecallType.NONE
    }
  }
}
