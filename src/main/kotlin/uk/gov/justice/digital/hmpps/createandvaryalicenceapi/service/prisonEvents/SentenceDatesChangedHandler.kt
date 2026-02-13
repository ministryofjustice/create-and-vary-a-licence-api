package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeactivateLicenceAndVariationsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeativationReason
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import java.time.LocalDate

@Service
class SentenceDatesChangedHandler(
  private val objectMapper: ObjectMapper,
  private val licenceRepository: LicenceRepository,
  private val licenceService: LicenceService,
  private val prisonService: PrisonService,
  private val updateSentenceDateService: UpdateSentenceDateService,
) {
  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun handleEvent(message: String) {
    log.info("Received sentence dates changed event")

    val event = try {
      objectMapper.readValue(message, SentenceDatesChangedEvent::class.java)
    } catch (e: JacksonException) {
      log.error("Failed to parse sentence dates change event message", e)
      throw e
    }
    val bookingId = event.bookingId
    val nomisId = event.offenderIdDisplay ?: prisonService.searchPrisonersByBookingIds(listOf(bookingId))
      .first().prisonerNumber

    val activeLicence = getActiveLicence(nomisId)
    if (activeLicence != null) {
      deactivateLicencesIfPrisonerResentenced(activeLicence, bookingId)
      deactivateLicencesIfFuturePrrd(activeLicence)
    } else {
      updateSentenceDates(nomisId)
    }
  }

  private fun deactivateLicencesIfPrisonerResentenced(licence: Licence, bookingId: Long) {
    val ssd = prisonService.getPrisonerLatestSentenceStartDate(bookingId)
    val lsd = licence.licenceStartDate

    if (ssd != null && lsd != null && ssd.isAfter(lsd)) {
      licenceService.deactivateLicenceAndVariations(
        licence.id,
        DeactivateLicenceAndVariationsRequest(reason = DateChangeLicenceDeativationReason.RESENTENCED),
      )
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
        licenceService.deactivateLicenceAndVariations(
          licence.id,
          DeactivateLicenceAndVariationsRequest(reason = DateChangeLicenceDeativationReason.RECALLED),
        )
      }
    }
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
