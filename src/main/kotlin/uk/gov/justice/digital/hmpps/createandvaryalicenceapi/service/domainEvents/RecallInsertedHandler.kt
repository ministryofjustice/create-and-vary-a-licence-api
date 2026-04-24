package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeactivationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE

@Service
class RecallInsertedHandler(
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonService: PrisonService,
  @param:Value("\${feature.toggle.standardRecalls.enabled:false}") private val standardRecallsEnabled: Boolean = false,
) {
  companion object {
    private val log = LoggerFactory.getLogger(RecallInsertedHandler::class.java)
  }

  @Transactional
  fun handleEvent(message: String) {
    val event = try {
      mapper.readValue(message, HMPPSDomainEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse recall inserted event message", e)
      throw e
    }

    val nomisId = event.personReference.noms()
    if (nomisId.isNullOrBlank()) {
      log.error("No nomis id found in recall inserted event: $message")
      return
    }

    log.info("Processing recall inserted event received for nomis id: $nomisId")
    val nomisRecord = prisonService.searchPrisonersByNomisIds(listOf(nomisId)).first()
    val activeLicence = getActiveLicence(nomisId)
    if (activeLicence != null) {
      log.info("nomisId: $nomisId, has active licence: ${activeLicence.id}")

      val recallType = prisonService.getRecallType(bookingId = nomisRecord.bookingId?.toLong()!!)
      log.info("nomisId: $nomisId, has recall type: $recallType, standard recalls enabled: $standardRecallsEnabled, ${recallType == RecallType.STANDARD}")
      if (recallType == RecallType.STANDARD && standardRecallsEnabled) {
        log.info("deactivating licence: ${activeLicence.id} due to STANDARD recall, reason ${DateChangeLicenceDeactivationReason.STANDARD_RECALL.message}")
        licenceService.deactivateLicenceAndVariations(
          activeLicence.id,
          DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeactivationReason.STANDARD_RECALL),
        )
      } else if (recallType == RecallType.FIXED_TERM) {
        log.info("deactivating licence: ${activeLicence.id} due to FIXED_TERM recall, reason ${DateChangeLicenceDeactivationReason.RECALLED.message}")
        licenceService.deactivateLicenceAndVariations(
          activeLicence.id,
          DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeactivationReason.RECALLED),
        )
      }
    }
  }

  private fun getActiveLicence(nomisId: String): Licence? = licenceRepository.findAllByNomsIdAndStatusCodeIn(
    nomisId,
    listOf(
      ACTIVE,
    ),
  ).firstOrNull()
}
