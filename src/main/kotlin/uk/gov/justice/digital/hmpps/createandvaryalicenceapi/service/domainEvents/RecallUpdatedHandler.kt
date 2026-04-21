package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.core.JacksonException
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

@Service
class RecallUpdatedHandler(
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonService: PrisonService,
  @param:Value("\${feature.toggle.standardRecalls.enabled:false}") private val standardRecallsEnabled: Boolean = false,
) {
  companion object {
    private val log = LoggerFactory.getLogger(RecallUpdatedHandler::class.java)
  }

  @Transactional
  fun handleEvent(message: String) {
    val event = try {
      mapper.readValue(message, HMPPSDomainEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse recall inserted updated message", e)
      throw e
    }

    val nomisId = event.personReference.noms()
    if (nomisId.isNullOrBlank()) {
      throw IllegalStateException("No nomis id found in recall updated event: $message")
    }

    log.info("Processing recall updated event for nomis id: $nomisId")
    val nomisRecord = prisonService.searchPrisonersByNomisIds(listOf(nomisId)).first()
    val bookingId = nomisRecord.bookingId?.toLong()!!
    val recallType = prisonService.getRecallType(bookingId)
    if (recallType == RecallType.NONE || (recallType == RecallType.STANDARD && !standardRecallsEnabled)) {
      return
    }

    val licencesToUpdate = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
      bookingId,
      listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
      listOf(LicenceKind.PRRD),
    )
    val updatedEligibleKind = if (recallType == RecallType.STANDARD) EligibleKind.STANDARD else EligibleKind.FIXED_TERM
    licencesToUpdate.forEach { licence ->
      licenceService.updateLicenceKind(
        licence,
        LicenceKind.PRRD,
        updatedEligibleKind,
      )
    }
  }
}
