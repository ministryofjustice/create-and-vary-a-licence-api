package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDate

@Service
class LicenceActivationService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonApiClient: PrisonApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val iS91DeterminationService: IS91DeterminationService,
  private val auditEventRepository: AuditEventRepository,
) {

  @Transactional
  fun licenceActivationJob() {
    val potentialLicences = licenceRepository.getApprovedLicencesOnOrPassedReleaseDate()
    if (potentialLicences.isEmpty()) {
      return
    }
    val (eligibleLicences, ineligibleLicences) = determineLicenceEligibility(potentialLicences)
    val (iS91licencesToActivate, standardLicencesToActivate) = determineEligibleLicencesToActivate(eligibleLicences)

    licenceService.activateLicences(iS91licencesToActivate, "IS91 licence automatically activated via repeating job")
    licenceService.activateLicences(standardLicencesToActivate, "Licence automatically activated via repeating job")
    licenceService.inactivateLicences(
      ineligibleLicences,
      "Licence automatically deactivated as booking ID has approved HDC licence",
    )
  }

  private fun determineLicenceEligibility(licences: List<Licence>): Pair<List<Licence>, List<Licence>> {
    val bookingIds = licences.map { it.bookingId!! }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingIds)
    val approvedHdcBookingIds = hdcStatuses.filter { it.approvalStatus == "APPROVED" }.map { it.bookingId }
    val (ineligibleLicences, eligibleLicences) = licences.partition {
      approvedHdcBookingIds.contains(it.bookingId)
    }
    return Pair(eligibleLicences, ineligibleLicences)
  }

  private fun determineEligibleLicencesToActivate(licences: List<Licence>): Pair<List<Licence>, List<Licence>> {
    val (iS91Licences, standardLicences) = filterLicencesIntoTypes(licences)
    val standardLicencesPrisoners =
      prisonerSearchApiClient.searchPrisonersByBookingIds(standardLicences.map { it.bookingId!! })
    val iS91LicencesToActivate = iS91Licences.filter { it.isPassedIS91ReleaseDate() }
    val standardLicencesToActivate =
      standardLicences.filter { it.isStandardLicenceForActivation(standardLicencesPrisoners) }

    return Pair(iS91LicencesToActivate, standardLicencesToActivate)
  }

  private fun filterLicencesIntoTypes(licences: List<Licence>): Pair<List<Licence>, List<Licence>> {
    val licenceBookingIds = licences.map { it.bookingId!! }
    val iS91AndExtraditionBookingIds = iS91DeterminationService.getIS91AndExtraditionBookingIds(licenceBookingIds)
    val (iS91AndExtraditionLicences, standardLicences) =
      licences.partition { iS91AndExtraditionBookingIds.contains(it.bookingId) }
    return Pair(iS91AndExtraditionLicences, standardLicences)
  }

  private fun Licence.isPassedIS91ReleaseDate(): Boolean {
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

  private fun Licence.isStandardLicenceForActivation(prisoners: List<PrisonerSearchPrisoner>): Boolean {
    val prisoner = prisoners.firstOrNull {
      it.prisonerNumber == this.nomsId
    }

    if (prisoner == null) {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = this.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Unable to find offender for ${this.forename} ${this.surname}, licence not activated",
          detail = "ID ${this.id} type ${this.typeCode} status ${this.statusCode.name} version ${this.version}",
        ),
      )

      return false
    }

    return (
      this.actualReleaseDate != null &&
        this.actualReleaseDate <= LocalDate.now() &&
        prisoner.status?.startsWith("INACTIVE") == true
      )
  }
}
