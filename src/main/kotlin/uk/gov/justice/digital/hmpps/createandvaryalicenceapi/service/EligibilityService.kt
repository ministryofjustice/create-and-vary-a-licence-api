package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

@Service
class EligibilityService(
  private val clock: Clock,
) {

  fun isEligibleForCvl(prisoner: PrisonerSearchPrisoner): Boolean {
    return !prisoner.isPersonParoleEligible() &&
      prisoner.hasCorrectLegalStatus() &&
      !prisoner.isOnIndeterminateSentence() &&
      prisoner.hasConditionalReleaseDate() &&
      prisoner.isEligibleIfOnAnExtendedDeterminateSentence() &&
      prisoner.hasActivePrisonStatus() &&
      prisoner.hasEligibleReleaseDate() &&
      !prisoner.isRecallCase()
  }

  private fun PrisonerSearchPrisoner.isPersonParoleEligible(): Boolean {
    if (this.paroleEligibilityDate != null) {
      if (this.paroleEligibilityDate.isAfter(LocalDate.now(clock))) {
        return true
      }
    }
    return false
  }

  private fun PrisonerSearchPrisoner.hasCorrectLegalStatus(): Boolean {
    return this.legalStatus != "DEAD"
  }

  private fun PrisonerSearchPrisoner.isOnIndeterminateSentence(): Boolean {
    return this.indeterminateSentence
  }

  private fun PrisonerSearchPrisoner.hasConditionalReleaseDate(): Boolean {
    return this.conditionalReleaseDate != null
  }

  private fun PrisonerSearchPrisoner.isEligibleIfOnAnExtendedDeterminateSentence(): Boolean {
    // If you don’t have a PED, you automatically pass this check as you’re not an EDS case
    if (this.paroleEligibilityDate == null) {
      return true
    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (this.confirmedReleaseDate != null) {
      val dateStart = this.conditionalReleaseDate!!.minusDays(4)
      if (this.confirmedReleaseDate.isBefore(dateStart) || this.confirmedReleaseDate.isAfter(this.conditionalReleaseDate)) {
        return false
      }
    }

    // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so not eligible
    if (this.actualParoleDate != null) {
      return false
    }

    return true
  }

  private fun PrisonerSearchPrisoner.hasActivePrisonStatus() = status?.let {
    it.startsWith("ACTIVE") || it == "INACTIVE TRN"
  } ?: false

  private fun PrisonerSearchPrisoner.hasEligibleReleaseDate(): Boolean {
    val releaseDate = this.confirmedReleaseDate ?: this.conditionalReleaseDate

    return releaseDate!!.isEqual(LocalDate.now(clock)) || releaseDate.isAfter(LocalDate.now(clock))
  }

  private fun PrisonerSearchPrisoner.isRecallCase(): Boolean {
    // If a CRD but no PRRD it should NOT be treated as a recall
    if (this.conditionalReleaseDate != null && this.postRecallReleaseDate == null) {
      return false
    }

    if (this.conditionalReleaseDate != null && this.postRecallReleaseDate != null) {
      // If the PRRD > CRD - it should be treated as a recall otherwise it is not treated as a recall
      return this.postRecallReleaseDate.isAfter(this.conditionalReleaseDate)
    }

    // Trust the Nomis recall flag as a fallback position - the above rules should always override
    return this.recall
  }
}
