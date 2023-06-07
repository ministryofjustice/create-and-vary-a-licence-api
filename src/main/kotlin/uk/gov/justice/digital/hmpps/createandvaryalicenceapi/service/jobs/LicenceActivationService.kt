package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate

@Service
class LicenceActivationService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient
) {

  @Transactional
  fun licenceActivationJob() {
    val potentialLicences = licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()
    val (eligibleLicences, ineligibleLicences) = determineLicenceEligibility(potentialLicences)
    val licencesToActivate = determineEligibleLicencesToActivate(eligibleLicences)

    if (licencesToActivate.any()) {
      licenceService.activateLicences(licencesToActivate)
    }
    if (ineligibleLicences.any()) {
      licenceService.inactivateLicences(ineligibleLicences)
    }
  }

  private fun determineLicenceEligibility(licences: List<Licence>): Pair<List<Licence>, List<Licence>> {
    val bookingIds = licences.map { it.bookingId!! }
    val hdcStatuses = prisonApiClient.hdcStatuses(bookingIds)
    val approvedHdcBookingIds = hdcStatuses.filter { it.approvalStatus == "APPROVED" }.map { it.bookingId }
    val (ineligibleLicences, eligibleLicences) = licences.partition {
      approvedHdcBookingIds.contains(it.bookingId)
    }
    return Pair(eligibleLicences, ineligibleLicences)
  }

  private fun determineEligibleLicencesToActivate(licences: List<Licence>): List<Licence> {
    val (iS91Licences, standardLicences) = filterLicencesIntoTypes(licences)
    val standardLicencesPrisoners =
      prisonerSearchApiClient.searchPrisonersByBookingIds(standardLicences.map { it.bookingId!! })

    val iS91LicencesToActivate = iS91Licences.filter { it.passedIS91ReleaseDate() }
    val standardLicencesToActivate =
      standardLicences.filter { it.standardLicenceForActivation(standardLicencesPrisoners) }

    return iS91LicencesToActivate + standardLicencesToActivate
  }

  private fun filterLicencesIntoTypes(licences: List<Licence>): Pair<List<Licence>, List<Licence>> {
    val licenceBookingIds = licences.map { it.bookingId!! }
    val iS91AndExtraditionBookingIds = prisonApiClient.getIS91AndExtraditionBookingIds(licenceBookingIds)
    val (iS91AndExtraditionLicences, standardLicences) =
      licences.partition { iS91AndExtraditionBookingIds.contains(it.bookingId) }
    return Pair(iS91AndExtraditionLicences, standardLicences)
  }

  private fun Licence.passedIS91ReleaseDate(): Boolean {
    if (this.conditionalReleaseDate == null) {
      return false
    }
    // If ARD within CRD minus 4 days and CRD (inclusive), use ARD
    val releaseDate = if (this.actualReleaseDate != null &&
      !this.actualReleaseDate.isBefore(this.conditionalReleaseDate.minusDays(4)) &&
      !this.actualReleaseDate.isAfter(this.conditionalReleaseDate)
    ) {
      this.actualReleaseDate
    } else {
      this.conditionalReleaseDate
    }

    return releaseDate <= LocalDate.now()
  }

  private fun Licence.standardLicenceForActivation(prisoners: List<PrisonerSearchPrisoner>): Boolean {
    val prisoner: PrisonerSearchPrisoner = prisoners.first {
      it.prisonerNumber == this.nomsId
    }
    if (this.actualReleaseDate != null &&
      this.actualReleaseDate <= LocalDate.now() &&
      prisoner.status?.startsWith("INACTIVE") == true
    ) {
      return true
    }
    return false
  }
}
