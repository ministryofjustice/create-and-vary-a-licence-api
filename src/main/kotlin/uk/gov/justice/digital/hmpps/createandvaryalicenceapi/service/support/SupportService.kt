package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.RecallSupportInfo
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner

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
    val prisoner = getPrisonerByPrisonNumber(prisonNumber)
    if (prisoner.bookingId == null) {
      error("Prison number $prisonNumber has no booking id")
    }

    val bookingSentenceAndRecallTypes = prisonService.getSentenceAndRecallTypes(prisoner.bookingId.toLong())

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

  private fun getPrisonerByPrisonNumber(prisonNumber: String): PrisonerSearchPrisoner {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    if (prisoners.size != 1) {
      error("Found ${prisoners.size} prisoners for: $prisonNumber")
    }
    return prisoners.first()
  }
}
