package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.LocalDate

@Service
class EligibilityService(
  private val prisonApiClient: PrisonApiClient,
  private val clock: Clock,
) {

  fun isEligibleForCvl(prisoner: PrisonerSearchPrisoner): Boolean {
    return !isPersonParoleEligible(prisoner.paroleEligibilityDate) &&
      hasCorrectLegalStatus(prisoner.legalStatus) &&
      !isOnIndeterminateSentence(prisoner.indeterminateSentence) &&
      hasConditionalReleaseDate(prisoner.conditionalReleaseDate) &&
      isOnEligibleExtendedDeterminateSentence(
        prisoner.paroleEligibilityDate,
        prisoner.conditionalReleaseDate,
        prisoner.confirmedReleaseDate,
        prisoner.actualParoleDate,
      ) &&
      hasActivePrisonStatus(prisoner.status) &&
      hasEligibleReleaseDate(prisoner.confirmedReleaseDate, prisoner.conditionalReleaseDate) &&
      !isRecallCase(prisoner.conditionalReleaseDate, prisoner.postRecallReleaseDate) &&
      !isHomeDetentionCurfewCase(prisoner.bookingId, prisoner.homeDetentionCurfewEligibilityDate)
  }

  private fun isPersonParoleEligible(paroleEligibilityDate: LocalDate?): Boolean {
    if (paroleEligibilityDate != null) {
      if (paroleEligibilityDate.isAfter(LocalDate.now(clock))) {
        return true
      }
    }
    return false
  }

  private fun hasCorrectLegalStatus(legalStatus: String): Boolean {
    return legalStatus != "DEAD"
  }

  private fun isOnIndeterminateSentence(isOnIndeterminateSentence: Boolean): Boolean {
    return isOnIndeterminateSentence
  }

  private fun hasConditionalReleaseDate(conditionalReleaseDate: LocalDate?): Boolean {
    return conditionalReleaseDate != null
  }

  private fun isOnEligibleExtendedDeterminateSentence(
    paroleEligibilityDate: LocalDate?,
    conditionalReleaseDate: LocalDate?,
    actualReleaseDate: LocalDate?,
    actualParoleDate: LocalDate?,
  ): Boolean {
    // If you don’t have a PED, you automatically pass this check as you’re not an EDS case
    if (paroleEligibilityDate == null) {
      return true
    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (actualReleaseDate != null) {
      val dateStart = conditionalReleaseDate!!.minusDays(4)
      if (!(actualReleaseDate.isAfter(dateStart) && (actualReleaseDate.isBefore(conditionalReleaseDate) || actualReleaseDate.isEqual(conditionalReleaseDate)))) {
        return false
      }
    }

    // an APD with a PED in the past means they were a successful parole applicant on a later attempt, so not eligible
    if (actualParoleDate != null) {
      return false
    }

    return true
  }

  private fun hasActivePrisonStatus(status: String?): Boolean {
    if (status != null) {
      if (status.startsWith("ACTIVE") || status == "INACTIVE TRN") {
        return true
      }
    }
    return false
  }

  private fun hasEligibleReleaseDate(actualReleaseDate: LocalDate?, conditionalReleaseDate: LocalDate?): Boolean {
    val releaseDate = actualReleaseDate ?: conditionalReleaseDate

    return releaseDate!!.isEqual(LocalDate.now(clock)) || releaseDate.isAfter(LocalDate.now(clock))
  }

  private fun isRecallCase(conditionalReleaseDate: LocalDate?, postRecallReleaseDate: LocalDate?): Boolean {
    // If a CRD but no PRRD it should NOT be treated as a recall
    if (postRecallReleaseDate == null) {
      return false
    }
    // If the PRRD > CRD - it should be treated as a recall otherwise it is not treated as a recall
    return postRecallReleaseDate.isAfter(conditionalReleaseDate!!)
  }

  private fun isHomeDetentionCurfewCase(bookingId: String, homeDetentionCurfewEligibilityDate: LocalDate?): Boolean {
    val bookingIdRequest = bookingId.toLong()
    val hdcStatus = prisonApiClient.getHdcStatus(bookingIdRequest).block()
    return (hdcStatus?.approvalStatus == "APPROVED" || homeDetentionCurfewEligibilityDate != null)
  }
}
