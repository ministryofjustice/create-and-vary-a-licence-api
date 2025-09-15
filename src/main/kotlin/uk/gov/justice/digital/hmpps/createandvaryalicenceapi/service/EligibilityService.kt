package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

typealias EligibilityCheck = (PrisonerSearchPrisoner) -> Boolean
typealias EligibilityCheckAndReason = Pair<EligibilityCheck, String>

operator fun EligibilityCheck.not(): EligibilityCheck = { !this(it) }
infix fun EligibilityCheck.describedAs(message: String): EligibilityCheckAndReason = this to message

fun Collection<EligibilityCheckAndReason>.getIneligibilityReasons(prisoner: PrisonerSearchPrisoner) = mapNotNull { (test, message) -> if (!test(prisoner)) message else null }

@Service
class EligibilityService(
  private val clock: Clock,
  @param:Value("\${recall.enabled}") private val recallEnabled: Boolean = false,
  @param:Value("\${recall.prisons}") private val recallEnabledPrisons: List<String> = emptyList(),
  @param:Value("\${recall.regions}") private val recallEnabledRegions: List<String> = emptyList(),
) {

  val genericChecks = listOf(
    !isPersonParoleEligible() describedAs "is eligible for parole",
    isNotDead() describedAs "has died",
    !isOnIndeterminateSentence() describedAs "is on indeterminate sentence",
    hasActivePrisonStatus() describedAs "is not active in prison",
    !isBreachOfTopUpSupervision() describedAs "is breach of top up supervision case",
  )

  val crdChecks = listOf(
    hasConditionalReleaseDate() describedAs "has no conditional release date",
    hasCrdTodayOrInTheFuture() describedAs "CRD in the past",
    isEligibleIfOnAnExtendedDeterminateSentence() describedAs "is on non-eligible EDS",
    !isRecallCase() describedAs "is a recall case",
  )

  val recallChecks = listOf(
    hasPostRecallReleaseDate() describedAs "has no post recall release date",
    hasPrrdTodayOrInTheFuture() describedAs "post recall release date is in the past",
    prrdIsBeforeSled() describedAs "post recall release date is not before SLED",
  )

  fun isEligibleForCvl(prisoner: PrisonerSearchPrisoner, areaCode: String? = null): Boolean = getIneligibilityReasons(prisoner, areaCode).isEmpty()

  fun getIneligibilityReasons(prisoner: PrisonerSearchPrisoner, areaCode: String? = null): List<String> {
    val genericIneligibilityReasons = genericChecks.getIneligibilityReasons(prisoner)
    val crdIneligibilityReasons = crdChecks.getIneligibilityReasons(prisoner)

    // If eligible for CRD licence, return as eligible
    if (genericIneligibilityReasons.isEmpty() && crdIneligibilityReasons.isEmpty()) {
      return emptyList()
    }

    var recallIneligibilityReasons = emptyList<String>()

    if (recallEnabled || (recallEnabledPrisons.contains(prisoner.prisonId) && recallEnabledRegions.contains(areaCode))) {
      recallIneligibilityReasons = recallChecks.getIneligibilityReasons(prisoner)
      // If eligible for PRRD licence, return as eligible
      if (genericIneligibilityReasons.isEmpty() && recallIneligibilityReasons.isEmpty()) {
        return emptyList()
      }
    }

    // If not eligible for either, return the combined list of reasons
    return (genericIneligibilityReasons + crdIneligibilityReasons + recallIneligibilityReasons).distinct()
  }

  // CRD-specific eligibility rules
  private fun hasConditionalReleaseDate(): EligibilityCheck = { it.conditionalReleaseDate != null }

  private fun hasCrdTodayOrInTheFuture(): EligibilityCheck = early@{
    val releaseDate = it.conditionalReleaseDate ?: return@early true
    releaseDate.isEqual(LocalDate.now(clock)) || releaseDate.isAfter(LocalDate.now(clock))
  }

  private fun isRecallCase(): EligibilityCheck = early@{
    // If a CRD but no PRRD it should NOT be treated as a recall
    if (it.conditionalReleaseDate != null && it.postRecallReleaseDate == null) {
      return@early false
    }

    if (it.conditionalReleaseDate != null && it.postRecallReleaseDate != null) {
      // If the PRRD > CRD - it should be treated as a recall otherwise it is not treated as a recall
      return@early it.postRecallReleaseDate.isAfter(it.conditionalReleaseDate)
    }

    // Trust the Nomis recall flag as a fallback position - the above rules should always override
    if (it.recall == null) {
      log.warn("${it.prisonerNumber} missing recall flag")
    }
    return@early it.recall ?: false
  }

  private fun isEligibleIfOnAnExtendedDeterminateSentence(): EligibilityCheck = early@{
    // If you don’t have a PED, you automatically pass this check as you’re not an EDS case
    if (it.paroleEligibilityDate == null) {
      return@early true
    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (it.confirmedReleaseDate != null && it.conditionalReleaseDate != null) {
      val dateStart = it.conditionalReleaseDate.minusDays(4)
      if (it.confirmedReleaseDate.isBefore(dateStart) || it.confirmedReleaseDate.isAfter(it.conditionalReleaseDate)) {
        return@early false
      }
    }

    // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so not eligible
    if (it.actualParoleDate != null) {
      return@early false
    }

    return@early true
  }

  // PRRD-specific eligibility rules
  private fun hasPostRecallReleaseDate(): EligibilityCheck = { it.postRecallReleaseDate != null }

  private fun hasPrrdTodayOrInTheFuture(): EligibilityCheck = early@{
    if (it.postRecallReleaseDate == null) return@early true

    dateIsTodayOrFuture(it.postRecallReleaseDate)
  }

  private fun prrdIsBeforeSled(): EligibilityCheck = early@{
    if (it.postRecallReleaseDate == null) return@early true

    it.postRecallReleaseDate.isBefore(it.licenceExpiryDate) && it.postRecallReleaseDate.isBefore(it.sentenceExpiryDate)
  }

  // Shared eligibility rules
  private fun isPersonParoleEligible(): EligibilityCheck = early@{
    if (it.paroleEligibilityDate != null) {
      if (it.paroleEligibilityDate.isAfter(LocalDate.now(clock))) {
        return@early true
      }
    }
    return@early false
  }

  private fun isNotDead(): EligibilityCheck = { it.legalStatus != "DEAD" }

  private fun isOnIndeterminateSentence(): EligibilityCheck = {
    if (it.indeterminateSentence == null) {
      log.warn("${it.prisonerNumber} missing indeterminateSentence")
    }
    it.indeterminateSentence ?: false
  }

  private fun hasActivePrisonStatus(): EligibilityCheck = { prisoner ->
    prisoner.status?.let {
      it.startsWith("ACTIVE") || it == "INACTIVE TRN"
    } ?: false
  }

  private fun isBreachOfTopUpSupervision(): EligibilityCheck = early@{
    it.imprisonmentStatus == "BOTUS"
  }

  private fun dateIsTodayOrFuture(date: LocalDate?): Boolean {
    if (date == null) return false
    return date.isEqual(LocalDate.now(clock)) || date.isAfter(LocalDate.now(clock))
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
