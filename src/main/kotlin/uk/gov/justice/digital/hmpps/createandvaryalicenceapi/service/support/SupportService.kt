package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.RecallSupportInfo
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.RemandSupportInfo
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.SupportInfo
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents

@Service
class SupportService(
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonService: PrisonService,
  private val eligibilityService: EligibilityService,
  private val iS91DeterminationService: IS91DeterminationService,
  private val hdcService: HdcService,
) {
  fun getIneligibilityReasons(prisonNumber: String): EligibilityAssessment {
    val prisoner = getPrisonerByPrisonNumber(prisonNumber)
    val hdcStatus = hdcService.getHdcStatus(listOf(prisoner))
    return eligibilityService.getEligibilityAssessment(prisoner, hdcStatus)
  }

  fun getIS91Status(prisonNumber: String): Boolean {
    val prisoner = getPrisonerByPrisonNumber(prisonNumber)
    return iS91DeterminationService.isIS91Case(prisoner)
  }

  fun getRecallInfo(prisonNumber: String): RecallSupportInfo {
    val bookingSentenceAndRecallTypes = prisonService.getSentenceAndRecallTypes(getBookingIdFromPrisonId(prisonNumber))

    val sentenceRecallTypes = bookingSentenceAndRecallTypes?.sentenceTypeRecallTypes.orEmpty()

    val (fixedTermRecalls, rest) = sentenceRecallTypes.partition { it.recallType.isFixedTermRecall }
    val (standardRecalls, otherSentences) = rest.partition { it.recallType.isStandardRecall }
    val recallName = (fixedTermRecalls + standardRecalls).firstOrNull()?.recallType?.recallName ?: "None"

    return RecallSupportInfo(
      recallType = prisonService.getRecallType(bookingSentenceAndRecallTypes),
      recallName = recallName,
      fixTermSentenceTypes = fixedTermRecalls.map { it.sentenceType },
      standardRecallSentenceTypes = standardRecalls.map { it.sentenceType },
      otherSentenceTypes = otherSentences.map { it.sentenceType }.distinct(),
    )
  }

  fun getRemandInfo(prisonerNumber: String): RemandSupportInfo {
    val remandCourtEventOutcomes =
      prisonService.getCourtOutcomeEvents(
        listOf(getBookingIdFromPrisonId(prisonerNumber)),
        RemandCourtEvents.getRemandCourtCodes(),
      )

    val outcomeReasonCode = remandCourtEventOutcomes.firstOrNull()?.outcomeReasonCode
      ?: return RemandSupportInfo()

    return RemandSupportInfo(
      true,
      outcomeReasonCode,
      RemandCourtEvents.getCourtEventDescriptionByCode(outcomeReasonCode),
    )
  }

  fun getSupportInfo(prisonerNumber: String): SupportInfo = SupportInfo(
    isIS91Case = getIS91Status(prisonerNumber),
    recallSupportInfo = getRecallInfo(prisonerNumber),
    remandSupportInfo = getRemandInfo(prisonerNumber),
  )

  private fun getBookingIdFromPrisonId(prisonerNumber: String): Long {
    val prisoner = getPrisonerByPrisonNumber(prisonerNumber)
    return prisoner.bookingId?.toLong() ?: error("Prison number $prisonerNumber has no booking id")
  }

  private fun getPrisonerByPrisonNumber(prisonNumber: String): PrisonerSearchPrisoner {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    return prisoners.first()
  }
}
