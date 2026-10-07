package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.PRE_RELEASE_STATUSES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE

const val DEACTIVATION_REASON = "deactivating unapproved licences on release"

@Service
class PrisonerReleasedHandler(
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val hdcService: HdcService,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  @param:Value("\${prisoner.released.handler.enabled:false}") private val handlerEnabled: Boolean = false,
) : EventHandler {

  private val log = LoggerFactory.getLogger(this::class.java)
  val activate = StatusUpdateRequest(ACTIVE, "SYSTEM", "SYSTEM")
  val inactivate = StatusUpdateRequest(INACTIVE, "SYSTEM", "SYSTEM")

  @Transactional
  override fun handleEvent(message: String) {
    if (!handlerEnabled) {
      log.info("Ignoring prisoner released event as handler is disabled")
      return
    }

    val event = try {
      mapper.readValue(message, HMPPSPrisonerReleasedEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse prisoner released event message", e)
      throw e
    }

    if (event.additionalInformation.reason != "RELEASED") {
      log.info("Received event for prisoner with reason: ${event.additionalInformation.reason}, skipping prisoner released event")
      return
    }

    val prisonNumber = event.additionalInformation.nomsNumber

    log.info("Processing prisoner released event received for prisonNumber: $prisonNumber")
    processLicencesOnRelease(prisonNumber)
  }

  @Transactional
  fun processLicencesOnRelease(prisonNumber: String) {
    val licences = this.licenceRepository.findAllByNomsIdAndStatusCodeIn(prisonNumber, PRE_RELEASE_STATUSES.toList())

    if (licences.isEmpty()) {
      return
    }

    val (approvedLicences, unapprovedLicences) = licences.partition { it.statusCode == APPROVED }
    check(approvedLicences.size <= 1) { "Multiple approved licences found, unable to automatically activate" }

    // Deactivate the unapproved set first. Activating or inactivating the approved licence below also cascades to
    // deactivate any related in-progress/submitted/timed-out versions of it, so deactivating those versions here
    // first (and flushing) ensures the cascade's status-based lookup can no longer select them, preventing duplicate
    // audit, licence and domain events for the same licence.
    if (unapprovedLicences.isNotEmpty()) {
      licenceService.inactivateLicences(unapprovedLicences, DEACTIVATION_REASON, deactivateInProgressVersions = true)
    }

    if (approvedLicences.isNotEmpty()) {
      val licenceToActivate = approvedLicences.first()
      val newStatus = if (isDeactivationRequired(prisonNumber, licenceToActivate)) inactivate else activate
      licenceService.updateLicenceStatus(licenceToActivate.id, newStatus)
    }
  }

  /*
   * Set the licence to ACTIVE unless any of the following scenarios are true:
   * 1. Licence is not Approved and can no longer be made active now the offender has been released.
   * 2. The offender has an approved HDC licence, which takes priority over the standard licence, since
   *    HDC licences indicate an early release from the prison, ahead of the standard licence
   */
  private fun isDeactivationRequired(prisonNumber: String, licence: Licence): Boolean {
    val prisoners = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(prisonNumber))
    check(prisoners.size == 1) { "a single prisoners wasn't found for: $prisonNumber (${prisoners.size} found)" }
    val hdcad = prisoners.first().homeDetentionCurfewActualDate

    return licence.kind !== HDC && hdcService.isApprovedForHdc(licence.bookingId!!, hdcad)
  }

  data class HMPPSPrisonerReleasedEvent(
    val eventType: String? = PRISON_OFFENDER_RELEASED_EVENT_TYPE,
    val additionalInformation: AdditionalInformationPrisonerReleased,
    val version: Int,
    val occurredAt: String,
    val description: String,
  )

  data class AdditionalInformationPrisonerReleased(
    val nomsNumber: String,
    val reason: String,
  )
}
