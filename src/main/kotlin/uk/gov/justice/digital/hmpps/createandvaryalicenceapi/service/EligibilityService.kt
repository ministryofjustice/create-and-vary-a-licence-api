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
    val cvlCheck = listOf(
      !isPersonParoleEligible(prisoner.paroleEligibilityDate),
      hasCorrectLegalStatus(prisoner.legalStatus),
      !isOnIndeterminateSentence(prisoner.indeterminateSentence),
      hasConditionalReleaseDate(prisoner.conditionalReleaseDate),
      isOnEligibleExtendedDeterminateSentence(
        prisoner.conditionalReleaseDate,
        prisoner.confirmedReleaseDate,
        prisoner.actualParoleDate,
      ),
      hasActivePrisonStatus(prisoner.status),
      hasEligibleReleaseDate(prisoner.confirmedReleaseDate, prisoner.conditionalReleaseDate),
      !isRecallCase(prisoner.conditionalReleaseDate, prisoner.postRecallReleaseDate),
    ).all { it }

    return if (!cvlCheck) {
      false
    } else {
      return !isHomeDetentionCurfewCase(prisoner.bookingId, prisoner.homeDetentionCurfewEligibilityDate)
    }
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
    conditionalReleaseDate: LocalDate?,
    actualReleaseDate: LocalDate?,
    actualParoleDate: LocalDate?,
  ): Boolean {
    // TODO - If you don’t have a PED, you automatically pass this check as you’re not an EDS case - already checked in isPersonParoleEligible
//    if (paroleEligibilityDate == null) {
//      return true
//    }

    // TODO - If you don't have a CRD, you are ineligible for CVL - covered by previous hasConditionalReleaseDate
//    if (conditionalReleaseDate == null) {
//      return false
//    }

    // TODO - duplicated check as already checking for this in isPersonParoleEligible
//    if (paroleEligibilityDate.isAfter(LocalDate.now())) {
//      return false
//    }

    // if ARD is not between CRD - 4 days and CRD inclusive (to account for bank holidays and weekends), not eligible
    if (actualReleaseDate != null && conditionalReleaseDate != null) {
      val dateStart = conditionalReleaseDate.minusDays(4)
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
    // TODO - according to the build create caseload - if the release date is before today, filtered out - is that correct?
//    if (actualReleaseDate == null){
//      if (conditionalReleaseDate != null){
//        return conditionalReleaseDate.isBefore(LocalDate.now())
//      }
//    }

    // TODO - if they have no confirmed release date and a CRD in the past should they be eligible?

    val releaseDate = actualReleaseDate ?: conditionalReleaseDate

    if (releaseDate != null) {
      return releaseDate.isEqual(LocalDate.now(clock)) || releaseDate.isAfter(LocalDate.now(clock))
    }
    return false
  }

  private fun isRecallCase(conditionalReleaseDate: LocalDate?, postRecallReleaseDate: LocalDate?): Boolean {
    return conditionalReleaseDate == null && postRecallReleaseDate != null
  }

  private fun isHomeDetentionCurfewCase(bookingId: String, homeDetentionCurfewEligibilityDate: LocalDate?): Boolean {
    val bookingIdRequest = bookingId.toLong()
    val hdcStatus = prisonApiClient.getHdcStatus(bookingIdRequest).block()
    return (hdcStatus?.approvalStatus == "APPROVED" || homeDetentionCurfewEligibilityDate != null)
  }
}
