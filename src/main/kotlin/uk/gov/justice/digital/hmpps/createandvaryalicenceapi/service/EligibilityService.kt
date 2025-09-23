package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

@Service
class EligibilityService(
  private val clock: Clock,
  @param:Value("\${recall.enabled}") private val recallEnabled: Boolean = false,
  @param:Value("\${recall.prisons}") private val recallEnabledPrisons: List<String> = emptyList(),
  @param:Value("\${recall.regions}") private val recallEnabledRegions: List<String> = emptyList(),
) {

  fun getEligibility(
    prisoners: List<PrisonerSearchPrisoner>,
    nomisIdsToAreaCodes: Map<String, String?>,
  ): Map<String, EligibilityAssessment> {
    return prisoners.map { prisoner ->
      val permitRecalls =
        (recallEnabled || (recallEnabledPrisons.contains(prisoner.prisonId) && recallEnabledRegions.contains(nomisIdsToAreaCodes[prisoner.prisonerNumber])))

      val genericIneligibilityReasons = getGenericIneligibilityReasons(prisoner)
      val crdIneligibilityReasons = getCrdIneligibilityReasons(prisoner)
      val prrdIneligibilityReasons = if (permitRecalls) getPrrdIneligibilityReasons(prisoner) else emptyList()

      println(!hasConditionalReleaseDate(prisoner))
      println(crdIneligibilityReasons)

      val isEligible = genericIneligibilityReasons.isEmpty() && (crdIneligibilityReasons.isEmpty() || (permitRecalls && prrdIneligibilityReasons.isEmpty()))

      return@map prisoner.prisonerNumber to EligibilityAssessment(
        genericIneligibilityReasons,
        crdIneligibilityReasons,
        prrdIneligibilityReasons,
        isEligible,
      )
    }.toMap()
  }

  fun isEligibleForCvl(prisoner: PrisonerSearchPrisoner, areaCode: String? = null): Boolean {
    val eligibility = getEligibility(listOf(prisoner), mapOf(prisoner.prisonerNumber to areaCode))[prisoner.prisonerNumber]!!
    return eligibility.isEligible
  }

  fun getIneligibilityReasons(prisoner: PrisonerSearchPrisoner, areaCode: String? = null): List<String> {
    val eligibility = getEligibility(listOf(prisoner), mapOf(prisoner.prisonerNumber to areaCode))[prisoner.prisonerNumber]!!
    return if (eligibility.isEligible) emptyList() else eligibility.genericIneligibilityReasons + eligibility.crdIneligibilityReasons + eligibility.prrdIneligibilityReasons
  }

  fun getGenericIneligibilityReasons(prisoner: PrisonerSearchPrisoner): List<String> {
    val eligibilityCriteria = listOf(
      !isPersonParoleEligible(prisoner) to "is eligible for parole",
      !isDead(prisoner) to "has died",
      !isOnIndeterminateSentence(prisoner) to "is on indeterminate sentence",
      hasActivePrisonStatus(prisoner) to "is not active in prison",
      !isBreachOfTopUpSupervision(prisoner) to "is breach of top up supervision case",
    )

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  fun getCrdIneligibilityReasons(prisoner: PrisonerSearchPrisoner): List<String> {
    val eligibilityCriteria = listOf(
      hasConditionalReleaseDate(prisoner) to "has no conditional release date",
      hasCrdTodayOrInTheFuture(prisoner) to "CRD in the past",
      isEligibleIfOnAnExtendedDeterminateSentence(prisoner) to "is on non-eligible EDS",
      !isRecallCase(prisoner) to "is a recall case",
    )

    println(eligibilityCriteria)

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  fun getPrrdIneligibilityReasons(prisoner: PrisonerSearchPrisoner): List<String> {
    val eligibilityCriteria = listOf(
      hasPostRecallReleaseDate(prisoner) to "has no post recall release date",
      hasPrrdTodayOrInTheFuture(prisoner) to "post recall release date is in the past",
      prrdIsBeforeSled(prisoner) to "post recall release date is not before SLED",
    )

    println(eligibilityCriteria)

    return eligibilityCriteria.mapNotNull { (test, message) -> if (!test) message else null }
  }

  // CRD-specific eligibility rules
  private fun hasConditionalReleaseDate(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.conditionalReleaseDate != null

  private fun hasCrdTodayOrInTheFuture(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.conditionalReleaseDate == null || dateIsTodayOrFuture(prisoner.conditionalReleaseDate)

  private fun isRecallCase(prisoner: PrisonerSearchPrisoner): Boolean {
    // If a CRD but no PRRD it should NOT be treated as a recall
    if (prisoner.conditionalReleaseDate != null && prisoner.postRecallReleaseDate == null) {
      return false
    }

    if (prisoner.conditionalReleaseDate != null && prisoner.postRecallReleaseDate != null) {
      // If the PRRD > CRD - it should be treated as a recall otherwise it is not treated as a recall
      return prisoner.postRecallReleaseDate.isAfter(prisoner.conditionalReleaseDate)
    }

    // Trust the Nomis recall flag as a fallback position - the above rules should always override
    if (prisoner.recall == null) {
      log.warn("${prisoner.prisonerNumber} missing recall flag")
    }
    return prisoner.recall ?: false
  }

  private fun isEligibleIfOnAnExtendedDeterminateSentence(prisoner: PrisonerSearchPrisoner): Boolean {
    // If you don’t have a PED, you automatically pass this check as you’re not an EDS case
    if (prisoner.paroleEligibilityDate == null) {
      return true
    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (prisoner.confirmedReleaseDate != null && prisoner.conditionalReleaseDate != null) {
      val dateStart = prisoner.conditionalReleaseDate.minusDays(4)
      if (prisoner.confirmedReleaseDate.isBefore(dateStart) || prisoner.confirmedReleaseDate.isAfter(prisoner.conditionalReleaseDate)) {
        return false
      }
    }

    // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so not eligible
    if (prisoner.actualParoleDate != null) {
      return false
    }

    return true
  }

  // PRRD-specific eligibility rules
  private fun hasPostRecallReleaseDate(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.postRecallReleaseDate != null

  private fun hasPrrdTodayOrInTheFuture(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.postRecallReleaseDate == null || dateIsTodayOrFuture(prisoner.postRecallReleaseDate)

  private fun prrdIsBeforeSled(prisoner: PrisonerSearchPrisoner): Boolean {
    if (prisoner.postRecallReleaseDate == null) return true

    return prisoner.postRecallReleaseDate.isBefore(prisoner.licenceExpiryDate) &&
      prisoner.postRecallReleaseDate.isBefore(
        prisoner.sentenceExpiryDate,
      )
  }

  // Shared eligibility rules
  private fun isPersonParoleEligible(prisoner: PrisonerSearchPrisoner): Boolean {
    if (prisoner.paroleEligibilityDate != null) {
      if (prisoner.paroleEligibilityDate.isAfter(LocalDate.now(clock))) {
        return true
      }
    }
    return false
  }

  private fun isDead(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.legalStatus == "DEAD"

  private fun isOnIndeterminateSentence(prisoner: PrisonerSearchPrisoner): Boolean {
    if (prisoner.indeterminateSentence == null) {
      log.warn("${prisoner.prisonerNumber} missing indeterminateSentence")
    }
    return prisoner.indeterminateSentence ?: false
  }

  private fun hasActivePrisonStatus(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.status?.let {
    it.startsWith("ACTIVE") || it == "INACTIVE TRN"
  } ?: false

  private fun isBreachOfTopUpSupervision(prisoner: PrisonerSearchPrisoner): Boolean = prisoner.imprisonmentStatus == "BOTUS"

  private fun dateIsTodayOrFuture(date: LocalDate?): Boolean {
    if (date == null) return false
    return date.isEqual(LocalDate.now(clock)) || date.isAfter(LocalDate.now(clock))
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
