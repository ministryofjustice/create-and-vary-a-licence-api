package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate

data class LicenceWithPrisoner(val licence: Licence, val prisoner: PrisonerSearchPrisoner) {
  val bookingId = licence.bookingId!!
  val homeDetentionCurfewEligibilityDate = prisoner.homeDetentionCurfewEligibilityDate
}

@Service
class LicenceActivationService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val iS91DeterminationService: IS91DeterminationService,
) {

  @Transactional
  fun licenceActivation() {
    val potentialLicences = licenceRepository.getApprovedLicencesOnOrPassedReleaseDate().associateBy { it.bookingId!! }
    if (potentialLicences.isEmpty()) {
      return
    }
    val matchedLicences = prisonerSearchApiClient.searchPrisonersByBookingIds(potentialLicences.keys)
      .map { LicenceWithPrisoner(potentialLicences[it.bookingId?.toLong()]!!, it) }

    val (eligibleLicences, ineligibleLicences) = determineLicenceEligibility(matchedLicences)
    val (iS91licencesToActivate, standardLicencesToActivate) = determineEligibleLicencesToActivate(eligibleLicences)

    licenceService.activateLicences(iS91licencesToActivate, IS91_LICENCE_ACTIVATION)
    licenceService.activateLicences(standardLicencesToActivate, LICENCE_ACTIVATION)
    licenceService.inactivateLicences(ineligibleLicences, LICENCE_DEACTIVATION)
  }

  private fun determineLicenceEligibility(matchedLicences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<Licence>> {
    val approvedHdcBookingIds = findBookingsWithHdc(matchedLicences)
    val (ineligibleLicences, eligibleLicences) = matchedLicences.partition { approvedHdcBookingIds.contains(it.bookingId) }
    return Pair(eligibleLicences, ineligibleLicences.map { it.licence })
  }

  private fun findBookingsWithHdc(matchedLicences: List<LicenceWithPrisoner>): List<Long> {
    val bookingsWithHdc = matchedLicences
      .filter { it.homeDetentionCurfewEligibilityDate != null }
      .map { it.bookingId }

    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return hdcStatuses.filter { it.approvalStatus == "APPROVED" }.mapNotNull { it.bookingId }
  }

  private fun determineEligibleLicencesToActivate(licences: List<LicenceWithPrisoner>): Pair<List<Licence>, List<Licence>> {
    val (iS91Licences, standardLicences) = filterLicencesIntoTypes(licences)
    val iS91LicencesToActivate = iS91Licences.filter { it.licence.isPassedIS91ReleaseDate() }
    val standardLicencesToActivate = standardLicences.filter { it.isStandardLicenceForActivation() }
    return Pair(iS91LicencesToActivate.map { it.licence }, standardLicencesToActivate.map { it.licence })
  }

  private fun filterLicencesIntoTypes(licences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val prisoners = licences.map { it.prisoner }
    val iS91RelatedIds = iS91DeterminationService.getIS91AndExtraditionBookingIds(prisoners)
    val (iS91AndExtraditionLicences, standardLicences) = licences.partition { iS91RelatedIds.contains(it.bookingId) }
    return Pair(iS91AndExtraditionLicences, standardLicences)
  }

  private fun Licence.isPassedIS91ReleaseDate(): Boolean {
    val actualReleaseDate = this.actualReleaseDate
    val conditionalReleaseDate = this.conditionalReleaseDate ?: return false

    // If ARD within CRD minus 4 days and CRD (inclusive), use ARD
    val releaseDate = if (actualReleaseDate != null &&
      !actualReleaseDate.isBefore(conditionalReleaseDate.minusDays(4)) &&
      !actualReleaseDate.isAfter(conditionalReleaseDate)
    ) {
      actualReleaseDate
    } else {
      conditionalReleaseDate
    }

    return releaseDate <= LocalDate.now()
  }

  private fun LicenceWithPrisoner.isStandardLicenceForActivation(): Boolean {
    return (
      licence.licenceStartDate != null &&
        licence.licenceStartDate!! <= LocalDate.now() &&
        prisoner.status?.startsWith("INACTIVE") == true
      )
  }

  companion object {
    const val IS91_LICENCE_ACTIVATION = "IS91 licence automatically activated via repeating job"
    const val LICENCE_ACTIVATION = "Licence automatically activated via repeating job"
    const val LICENCE_DEACTIVATION = "Licence automatically deactivated as booking ID has approved HDC licence"
  }
}
