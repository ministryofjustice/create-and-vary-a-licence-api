package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import com.fasterxml.jackson.core.JacksonException
import jakarta.transaction.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeactivationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate

@Service
class SentenceDatesChangedHandler(
  private val mapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonService: PrisonService,
  private val updateSentenceDateService: UpdateSentenceDateService,
  @param:Value("\${feature.toggle.standardRecalls.enabled:false}")
  private val areStandardRecallsEnabled: Boolean = false,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun handleEvent(message: String) {
    val event = try {
      mapper.readValue(message, SentenceDatesChangedEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse sentence dates change event message", e)
      throw e
    }
    val bookingId = event.bookingId
    val nomisId = event.offenderIdDisplay ?: prisonService.searchPrisonersByBookingIds(listOf(bookingId))
      .first().prisonerNumber
    log.info("Processing sentence dates changed event for bookingId: $bookingId, nomisId: $nomisId")

    val activeLicence = getActiveLicence(nomisId)
    if (activeLicence != null) {
      log.info("nomisId: $nomisId, has active licence: ${activeLicence.id}")
      processDeactivationChecks(activeLicence)
    } else {
      log.info("updating sentence dates for nomisId: $nomisId")
      updateSentenceDates(nomisId)
    }
  }

  private fun processDeactivationChecks(licence: Licence) {
    deactivateLicencesIfPrisonerResentenced(licence)
    deactivateLicencesIfFuturePrrd(licence)
    deactivateLicenceIfOnStandardRecall(licence)
  }

  private fun deactivateLicencesIfPrisonerResentenced(licence: Licence) {
    val ssd = prisonService.getPrisonerLatestSentenceStartDate(licence.bookingId!!)
    val lsd = licence.licenceStartDate

    log.info("Checking if prisoner resentenced, ssd: {}, lsd: {}", ssd, lsd)
    if (ssd != null && lsd != null && ssd.isAfter(lsd)) {
      deactivateLicenceAndVariations(licence.id, DateChangeLicenceDeactivationReason.RESENTENCED)
    }
  }

  private fun deactivateLicencesIfFuturePrrd(licence: Licence) {
    val prisoner = prisonService.getPrisonerDetail(licence.nomsId!!)

    val prrd = prisoner.sentenceDetail.postRecallReleaseOverrideDate ?: prisoner.sentenceDetail.postRecallReleaseDate
    if (prrd != null) {
      if (prrd == licence.postRecallReleaseDate) {
        return
      }
      if (prrd.isAfter(LocalDate.now())) {
        deactivateLicenceAndVariations(licence.id, DateChangeLicenceDeactivationReason.RECALLED)
      }
    }
  }

  private fun deactivateLicenceIfOnStandardRecall(licence: Licence) {
    if (areStandardRecallsEnabled) {
      if (prisonService.hasStandardRecallSentence(licence.bookingId!!)) {
        deactivateLicenceAndVariations(licence.id, DateChangeLicenceDeactivationReason.STANDARD_RECALL)
      }
    }
  }

  private fun deactivateLicenceAndVariations(licenceId: Long, reason: DateChangeLicenceDeactivationReason) {
    licenceService.deactivateLicenceAndVariations(
      licenceId,
      DeactivateLicenceAndVariationsRequest(reason),
    )
  }

  private fun updateSentenceDates(nomisId: String) {
    val licences = licenceRepository.findAllByNomsIdAndStatusCodeIn(
      nomisId,
      listOf(
        LicenceStatus.IN_PROGRESS,
        LicenceStatus.SUBMITTED,
        LicenceStatus.REJECTED,
        LicenceStatus.APPROVED,
        LicenceStatus.TIMED_OUT,
      ),
    )
    licences.forEach { licence -> updateSentenceDateService.updateSentenceDates(licence.id) }
  }

  private fun getActiveLicence(nomisId: String): Licence? = licenceRepository.findAllByNomsIdAndStatusCodeIn(
    nomisId,
    listOf(
      ACTIVE,
    ),
  ).firstOrNull()
}
