package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.PrisonInformationService.UpdateType.MOVEMENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES

@Service
class PrisonerReceivedHandler(
  private val mapper: ObjectMapper,
  private val prisonInformationService: PrisonInformationService,
  private val licenceRepository: LicenceRepository,
  @param:Value("\${feature.toggle.remand.enabled:false}")
  private val remandEnabled: Boolean,
) : EventHandler {

  private val log = LoggerFactory.getLogger(this::class.java)
  private val validMovementReasons = listOf("ADMISSION", "TRANSFERRED")

  @Transactional
  override fun handleEvent(message: String) {
    if (!remandEnabled) {
      log.info("Ignoring prisoner received event as handler is disabled")
      return
    }

    log.info("Received prisoner received event")
    val event = mapper.readValue(message, HMPPSDomainEvent::class.java)

    val additionalInformation = mapper.convertValue(event.additionalInformation, HMPPSPrisonerReceivedEvent::class.java)
    val reason = additionalInformation.reason

    if (reason !in validMovementReasons) {
      log.info("Received event for prisoner with reason: $reason, skipping prisoner received event")
      return
    }

    val nomsId = additionalInformation.nomsNumber
    val licences = getLicences(nomsId)

    if (licences.isEmpty()) {
      log.info("No licences found for nomsId: $nomsId, skipping prisoner received event")
      return
    }

    prisonInformationService.updatePrisonInformation(MOVEMENT, licences, additionalInformation.prisonId)
  }

  private fun getLicences(nomsId: String): List<Licence> = licenceRepository.findAllByNomsIdAndStatusCodeIn(nomsId, IN_FLIGHT_LICENCES.toList())

  private data class HMPPSPrisonerReceivedEvent(
    val reason: String,
    val prisonId: String,
    val nomsNumber: String,
  )
}
