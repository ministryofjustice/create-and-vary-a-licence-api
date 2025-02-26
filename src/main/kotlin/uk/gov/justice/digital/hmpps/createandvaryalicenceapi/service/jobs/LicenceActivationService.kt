package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
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
  private val hdcService: HdcService,
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
    val (eligibleLicences, ineligibleLicences) = determineActivationEligibility(matchedLicences)
    val (iS91licencesToActivate, standardLicencesToActivate) = findLicencesToActivate(eligibleLicences)

    licenceService.activateLicences(iS91licencesToActivate, IS91_LICENCE_ACTIVATION)
    licenceService.activateLicences(standardLicencesToActivate, LICENCE_ACTIVATION)
    licenceService.inactivateLicences(ineligibleLicences.map { it.licence }, LICENCE_DEACTIVATION)
  }

  private fun determineActivationEligibility(licences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val hdcStatus = hdcService.getHdcStatus(licences, { it.bookingId }, { it.homeDetentionCurfewEligibilityDate })

    // Filter out HDC licences that have not been approved for HDC as we don't want to deactivate them
    val filteredLicences = licences.filterNot { hdcStatus.isWaitingForActivation(it.licence.kind, it.bookingId) }

    return filteredLicences.partition { hdcStatus.canBeActivated(it.licence.kind, it.bookingId) }
  }

  private fun findLicencesToActivate(licences: List<LicenceWithPrisoner>): Pair<List<Licence>, List<Licence>> {
    val (iS91Licences, standardLicences) = filterLicencesIntoTypes(licences)
    val iS91LicencesToActivate = iS91Licences.filter { it.licence.isPassedIS91ReleaseDate() }
    val standardLicencesToActivate = standardLicences.filter { it.isStandardLicenceForActivation() }
    return iS91LicencesToActivate.map { it.licence } to standardLicencesToActivate.map { it.licence }
  }

  private fun filterLicencesIntoTypes(licences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val prisoners = licences.map { it.prisoner }
    val iS91RelatedIds = iS91DeterminationService.getIS91AndExtraditionBookingIds(prisoners)
    val (iS91AndExtraditionLicences, standardLicences) = licences.partition { iS91RelatedIds.contains(it.bookingId) }
    return iS91AndExtraditionLicences to standardLicences
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

  private fun LicenceWithPrisoner.isStandardLicenceForActivation(): Boolean = (
    licence.licenceStartDate != null &&
      licence.licenceStartDate!! <= LocalDate.now() &&
      prisoner.status?.startsWith("INACTIVE") == true
    )

  companion object {
    const val IS91_LICENCE_ACTIVATION = "IS91 licence automatically activated via repeating job"
    const val LICENCE_ACTIVATION = "Licence automatically activated via repeating job"
    const val LICENCE_DEACTIVATION = "Licence automatically deactivated as booking ID has approved HDC licence"
  }
}
