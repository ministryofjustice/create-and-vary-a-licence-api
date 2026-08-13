package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService.IS91Constants.IS91_RESULT_CODES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents
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
  private val telemetryService: TelemetryService,
  private val prisonApiClient: PrisonApiClient,
  @param:Value("\${feature.toggle.remand.enabled}") private val remandEnabled: Boolean = false,

) {

  @Transactional
  fun licenceActivation() {
    val potentialLicences = licenceRepository.getApprovedLicencesOnOrPassedReleaseDate().associateBy { it.bookingId!! }
    log.info("Licence activation job started: found ${potentialLicences.size} approved licences on or past release date")
    if (potentialLicences.isEmpty()) {
      return
    }
    val matchedLicences = prisonerSearchApiClient.searchPrisonersByBookingIds(potentialLicences.keys)
      .map { LicenceWithPrisoner(potentialLicences[it.bookingId?.toLong()]!!, it) }
    val (eligibleLicences, ineligibleLicences) = determineActivationEligibility(matchedLicences)
    val (iS91licencesToActivate, remandLicencesToActivate, standardLicencesToActivate) = findLicencesToActivate(eligibleLicences)

    log.info(
      "Licence activation job: activating ${iS91licencesToActivate.size} IS91 licences, " +
        "${remandLicencesToActivate.size} remand licences, " +
        "${standardLicencesToActivate.size} standard licences, " +
        "inactivating ${ineligibleLicences.size} licences",
    )

    licenceService.activateLicences(iS91licencesToActivate, IS91_LICENCE_ACTIVATION)
    licenceService.activateLicences(remandLicencesToActivate, REMAND_LICENCE_ACTIVATION)
    remandLicencesToActivate.forEach { licence ->
      telemetryService.recordLicenceForPrisonerOnRemandActivatedEvent(licence)
    }
    licenceService.activateLicences(standardLicencesToActivate, LICENCE_ACTIVATION)
    licenceService.inactivateLicences(ineligibleLicences.map { it.licence }, LICENCE_DEACTIVATION)
  }

  private fun determineActivationEligibility(licences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val hdcStatus = hdcService.getHdcStatus(licences, { it.bookingId }, { it.homeDetentionCurfewEligibilityDate })

    // Filter out HDC licences that have not been approved for HDC as we don't want to deactivate them
    val filteredLicences = licences.filterNot { hdcStatus.isWaitingForActivation(it.licence.kind, it.bookingId) }

    return filteredLicences.partition { hdcStatus.canBeActivated(it.licence.kind, it.bookingId) }
  }

  private fun findLicencesToActivate(licences: List<LicenceWithPrisoner>): Triple<List<Licence>, List<Licence>, List<Licence>> {
    val (iS91Licences, remandLicences, standardLicences) = filterLicencesIntoTypes(licences)
    val iS91LicencesToActivate = iS91Licences.filter { isPassedLicenceStartDate(it.licence.licenceStartDate) }
    val remandLicencesToActivate = remandLicences.filter { isPassedLicenceStartDate(it.licence.licenceStartDate) }
    val standardLicencesToActivate = standardLicences.filter { it.isStandardLicenceForActivation() }
    return Triple(iS91LicencesToActivate.map { it.licence }, remandLicencesToActivate.map { it.licence }, standardLicencesToActivate.map { it.licence })
  }

  private fun filterLicencesIntoTypes(licences: List<LicenceWithPrisoner>): Triple<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val prisoners = licences.map { it.prisoner }

    val(immigrationDetainees, nonImmigrationDetainees) = iS91DeterminationService.getImmigrationDetainees(prisoners)

    val immigrationDetaineeBookingIds = immigrationDetainees.mapNotNull { it.bookingId?.toLong() }
    val nonImmigrationBookingIds = nonImmigrationDetainees.mapNotNull { it.bookingId?.toLong() }

    val courtEventOutcomes = prisonApiClient.getCourtEventOutcomes(
      nonImmigrationBookingIds,
      if (remandEnabled) IS91_RESULT_CODES + RemandCourtEvents.getRemandCourtCodes() else IS91_RESULT_CODES,
    )

    val iS91OutcomeBookingIds = courtEventOutcomes
      .filter { it.outcomeReasonCode in IS91_RESULT_CODES }
      .map { it.bookingId }

    val remandOutcomeBookingIds = courtEventOutcomes
      .filter { it.outcomeReasonCode in RemandCourtEvents.getRemandCourtCodes() }
      .map { it.bookingId }

    val is91BookingIds = immigrationDetaineeBookingIds + iS91OutcomeBookingIds
    val (is91Licences, nonIs91Licences) = licences.partition { it.bookingId in is91BookingIds }
    val (remandLicences, standardLicences) = nonIs91Licences.partition { it.bookingId in remandOutcomeBookingIds }

    return Triple(is91Licences, remandLicences, standardLicences)
  }

  private fun LicenceWithPrisoner.isStandardLicenceForActivation(): Boolean = (
    isPassedLicenceStartDate(licence.licenceStartDate) &&
      prisoner.status?.startsWith("INACTIVE") == true
    )

  private fun isPassedLicenceStartDate(licenceStartDate: LocalDate?): Boolean = licenceStartDate != null && licenceStartDate <= LocalDate.now()

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val IS91_LICENCE_ACTIVATION = "IS91 licence automatically activated via repeating job"
    const val REMAND_LICENCE_ACTIVATION = "Remand licence automatically activated via repeating job"
    const val LICENCE_ACTIVATION = "Licence automatically activated via repeating job"
    const val LICENCE_DEACTIVATION = "Licence automatically deactivated as booking ID has approved HDC licence"
  }
}
