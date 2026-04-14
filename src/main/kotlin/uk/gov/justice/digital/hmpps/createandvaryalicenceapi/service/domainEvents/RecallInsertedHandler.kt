package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeactivationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE

@Service
class RecallInsertedHandler(
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonService: PrisonService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(PrisonerUpdatedHandler::class.java)
  }

  fun handleEvent(message: String) {
    val event = try {
      mapper.readValue(message, HMPPSDomainEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse event message", e)
      return
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
      val hasStandardRecall = prisonService.hasStandardRecallSentence(nomisRecord.bookingId?.toLong()!!)
      if (hasStandardRecall) {
        licenceService.deactivateLicenceAndVariations(
          activeLicence.id,
          DeactivateLicenceAndVariationsRequest(DateChangeLicenceDeactivationReason.STANDARD_RECALL),
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
