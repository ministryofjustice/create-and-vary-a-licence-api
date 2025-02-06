package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

typealias EligibilityCheck = (PrisonerSearchPrisoner) -> Boolean

operator fun EligibilityCheck.not(): EligibilityCheck = { !this(it) }
infix fun EligibilityCheck.describedAs(message: String): Pair<EligibilityCheck, String> = this to message

@Service
class EligibilityService(
  private val clock: Clock,
) {

  val checks = listOf(
    !isPersonParoleEligible() describedAs "is eligible for parole",
    isNotDead() describedAs "has died",
    !isOnIndeterminateSentence() describedAs "is on indeterminate sentence",
    hasConditionalReleaseDate() describedAs "has no conditional release date",
    isEligibleIfOnAnExtendedDeterminateSentence() describedAs "is on non-eligible EDS",
    hasActivePrisonStatus() describedAs "is not active in prison",
    hasReleaseDateInTheFuture() describedAs "release date in past",
    !isRecallCase() describedAs "is a recall case",
    !isBreachOfTopUpSupervision() describedAs "is breach of top up supervision case",
  )

  fun isEligibleForCvl(prisoner: PrisonerSearchPrisoner): Boolean {
    return getIneligibilityReasons(prisoner).isEmpty()
  }

  fun getIneligibilityReasons(prisoner: PrisonerSearchPrisoner): List<String> {
    return checks.mapNotNull { (test, message) -> if (!test(prisoner)) message else null }
  }

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

  private fun hasConditionalReleaseDate(): EligibilityCheck = { it.conditionalReleaseDate != null }

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

  private fun hasActivePrisonStatus(): EligibilityCheck = { prisoner ->
    prisoner.status?.let {
      it.startsWith("ACTIVE") || it == "INACTIVE TRN"
    } ?: false
  }

  private fun hasReleaseDateInTheFuture(): EligibilityCheck = early@{
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

  private fun isBreachOfTopUpSupervision(): EligibilityCheck = early@{
    it.imprisonmentStatus == "BOTUS"
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
