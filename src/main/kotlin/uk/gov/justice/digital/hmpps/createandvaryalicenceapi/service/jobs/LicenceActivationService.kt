package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService.IS91Constants.IS91_RESULT_CODES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService.IS91Constants.OFFENCE_DESCRIPTION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TelemetryService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents
import java.time.LocalDate

data class LicenceWithPrisoner(val licence: Licence, val prisoner: PrisonerSearchPrisoner) {
  val bookingId = prisoner.bookingId!!.toLong()
  val homeDetentionCurfewEligibilityDate = prisoner.homeDetentionCurfewEligibilityDate
}

@Service
class LicenceActivationService(
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val hdcService: HdcService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val auditService: AuditService,
  private val telemetryService: TelemetryService,
  @param:Value("\${feature.toggle.remand.enabled}") private val remandEnabled: Boolean = false,
) {
  @Transactional
  fun licenceActivation() {
    val potentialLicences = licenceRepository.getApprovedLicencesOnOrPassedReleaseDate().associateBy { it.nomsId!! }
    log.info("Licence activation job started: found ${potentialLicences.size} approved licences on or past release date")
    if (potentialLicences.isEmpty()) {
      return
    }

    val matchedLicences = prisonerSearchApiClient.searchPrisonersByNomisIds(potentialLicences.keys.toList())
      .map { LicenceWithPrisoner(potentialLicences[it.prisonerNumber]!!, it) }

    val (eligibleLicences, ineligibleLicences) = determineActivationEligibility(matchedLicences)
    val licencesToActivate = findLicencesToActivate(eligibleLicences)

    updateChangedBookings(
      licencesToActivate.iS91Licences + licencesToActivate.remandLicences + licencesToActivate.standardLicences,
    )

    val numIS91Licences = licencesToActivate.iS91Licences.size
    val numRemandLicences = licencesToActivate.remandLicences.size
    val numStandardLicences = licencesToActivate.standardLicences.size
    val numIneligibleLicences = ineligibleLicences.size
    log.info(
      "Licence activation job: activating $numIS91Licences IS91 licences, " +
        "$numRemandLicences remand licences, " +
        "$numStandardLicences standard licences, " +
        "$numIneligibleLicences ineligible licences",
    )

    licenceService.activateLicences(licencesToActivate.iS91Licences.map { it.licence }, IS91_LICENCE_ACTIVATION)
    licenceService.activateLicences(licencesToActivate.remandLicences.map { it.licence }, REMAND_LICENCE_ACTIVATION)
    licenceService.activateLicences(licencesToActivate.standardLicences.map { it.licence }, LICENCE_ACTIVATION)
    licenceService.inactivateLicences(ineligibleLicences.map { it.licence }, LICENCE_DEACTIVATION)

    telemetryService.recordActivateLicencesJobEvent(
      numIS91Licences,
      numRemandLicences,
      numStandardLicences,
      numIneligibleLicences,
    )
  }

  private fun determineActivationEligibility(licences: List<LicenceWithPrisoner>): Pair<List<LicenceWithPrisoner>, List<LicenceWithPrisoner>> {
    val hdcStatus = hdcService.getHdcStatus(licences, { it.bookingId }, { it.homeDetentionCurfewEligibilityDate })

    // Filter out HDC licences that have not been approved for HDC as we don't want to deactivate them
    val filteredLicences = licences.filterNot { hdcStatus.isWaitingForActivation(it.licence.kind, it.bookingId) }

    return filteredLicences.partition { hdcStatus.canBeActivated(it.licence.kind, it.bookingId) }
  }

  private fun findLicencesToActivate(licences: List<LicenceWithPrisoner>): LicencesToActivate {
    val licenceBuckets = filterLicencesIntoTypes(licences)
    return LicencesToActivate(
      iS91Licences = licenceBuckets.iS91Licences.filter { isPassedLicenceStartDate(it.licence.licenceStartDate) },
      remandLicences = licenceBuckets.remandLicences.filter { isPassedLicenceStartDate(it.licence.licenceStartDate) },
      standardLicences = licenceBuckets.standardLicences.filter { it.isStandardLicenceForActivation() },
    )
  }

  private fun filterLicencesIntoTypes(licences: List<LicenceWithPrisoner>): LicenceBuckets {
    val prisoners = licences.map { it.prisoner }

    val (immigrationDetainees, nonImmigrationDetainees) = prisoners.partition { it.mostSeriousOffence == OFFENCE_DESCRIPTION }

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

    val iS91BookingIds = immigrationDetaineeBookingIds + iS91OutcomeBookingIds
    val (iS91Licences, nonIs91Licences) = licences.partition { it.bookingId in iS91BookingIds }
    val (remandLicences, standardLicences) = nonIs91Licences.partition { it.bookingId in remandOutcomeBookingIds }

    return LicenceBuckets(iS91Licences, remandLicences, standardLicences)
  }

  private fun LicenceWithPrisoner.isStandardLicenceForActivation(): Boolean = (
    isPassedLicenceStartDate(licence.licenceStartDate) &&
      prisoner.status?.startsWith("INACTIVE") == true
    )

  private fun isPassedLicenceStartDate(licenceStartDate: LocalDate?): Boolean = licenceStartDate != null && licenceStartDate <= LocalDate.now()

  private fun updateChangedBookings(licences: List<LicenceWithPrisoner>) {
    val licencesWithChangedBooking = licences.filter { it.licence.bookingId != it.bookingId }

    licencesWithChangedBooking.map {
      val oldBookingId = it.licence.bookingId
      val oldBookingNo = it.licence.bookingNo

      it.licence.bookingId = it.bookingId
      it.licence.bookingNo = it.prisoner.bookNumber
      licenceRepository.save(it.licence)

      auditService.recordAuditEventBookingChanged(
        licence = it.licence,
        oldBookingId = oldBookingId,
        newBookingId = it.bookingId,
        oldBookingNo = oldBookingNo,
        newBookingNo = it.prisoner.bookNumber,
      )

      log.info("Updated booking for licence id ${it.licence.id}")
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val IS91_LICENCE_ACTIVATION = "IS91 licence automatically activated via repeating job"
    const val REMAND_LICENCE_ACTIVATION = "Remand licence automatically activated via repeating job"
    const val LICENCE_ACTIVATION = "Licence automatically activated via repeating job"
    const val LICENCE_DEACTIVATION = "Licence automatically deactivated as booking ID has approved HDC licence"
  }

  private data class LicenceBuckets(
    val iS91Licences: List<LicenceWithPrisoner>,
    val remandLicences: List<LicenceWithPrisoner>,
    val standardLicences: List<LicenceWithPrisoner>,
  )

  private data class LicencesToActivate(
    val iS91Licences: List<LicenceWithPrisoner>,
    val remandLicences: List<LicenceWithPrisoner>,
    val standardLicences: List<LicenceWithPrisoner>,
  )
}
