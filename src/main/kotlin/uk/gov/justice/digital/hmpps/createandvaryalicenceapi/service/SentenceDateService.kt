package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import java.time.LocalDate
import java.time.LocalDateTime
import javax.persistence.EntityNotFoundException

/**
 * Defines hooks for condition specific behaviour when sentence date changes occur
 */
interface DateChangeListener {
  /** Should this listener fire */
  fun isDateChangeRelevant(licence: Licence, sentenceChanges: SentenceChanges): Boolean

  /** Allow updating licence before persisting  */
  fun modify(licence: Licence, sentenceChanges: SentenceChanges): Licence = licence

  /** Allow performing an action after data has been flushed (but before transaction commit) */
  fun afterFlush(licence: Licence, sentenceChanges: SentenceChanges) {}
}

@Service
class SentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
  private val listeners: List<DateChangeListener>
) {

  @Transactional
  fun updateSentenceDates(licenceId: Long, sentenceDatesRequest: UpdateSentenceDatesRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    log.info(
      "Licence dates - ID $licenceId " +
        "CRD ${licenceEntity.conditionalReleaseDate} " +
        "ARD ${licenceEntity.actualReleaseDate} " +
        "SSD ${licenceEntity.sentenceStartDate} " +
        "SED ${licenceEntity.sentenceEndDate} " +
        "LSD ${licenceEntity.licenceStartDate} " +
        "LED ${licenceEntity.licenceExpiryDate} " +
        "TUSSD ${licenceEntity.topupSupervisionStartDate} " +
        "TUSED ${licenceEntity.topupSupervisionExpiryDate}"
    )

    log.info(
      "Event dates - ID $licenceId " +
        "CRD ${sentenceDatesRequest.conditionalReleaseDate} " +
        "ARD ${sentenceDatesRequest.actualReleaseDate} " +
        "SSD ${sentenceDatesRequest.sentenceStartDate} " +
        "SED ${sentenceDatesRequest.sentenceEndDate} " +
        "LSD ${sentenceDatesRequest.licenceStartDate} " +
        "LED ${sentenceDatesRequest.licenceExpiryDate} " +
        "TUSSD ${sentenceDatesRequest.topupSupervisionStartDate} " +
        "TUSED ${sentenceDatesRequest.topupSupervisionExpiryDate}"
    )

    val sentenceChanges = getSentenceChanges(sentenceDatesRequest, licenceEntity)

    var updatedLicenceEntity = licenceEntity.copy(
      statusCode = if (hasOffenderBeenReSentencedWithActiveLicence(
          sentenceDatesRequest,
          licenceEntity
        )
      ) INACTIVE else licenceEntity.statusCode,
      conditionalReleaseDate = sentenceDatesRequest.conditionalReleaseDate,
      actualReleaseDate = sentenceDatesRequest.actualReleaseDate,
      sentenceStartDate = sentenceDatesRequest.sentenceStartDate,
      sentenceEndDate = sentenceDatesRequest.sentenceEndDate,
      licenceStartDate = sentenceDatesRequest.licenceStartDate,
      licenceExpiryDate = sentenceDatesRequest.licenceExpiryDate,
      topupSupervisionStartDate = sentenceDatesRequest.topupSupervisionStartDate,
      topupSupervisionExpiryDate = sentenceDatesRequest.topupSupervisionExpiryDate,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username
    )

    val relevantListeners = listeners.filter { it.isDateChangeRelevant(updatedLicenceEntity, sentenceChanges) }
    relevantListeners.forEach {
      updatedLicenceEntity = it.modify(updatedLicenceEntity, sentenceChanges)
    }

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    auditEventRepository.saveAndFlush(
      transform(
        AuditEvent(
          licenceId = licenceEntity.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Sentence dates updated for ${licenceEntity.forename} ${licenceEntity.surname}",
          detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode} version ${licenceEntity.version}",
        )
      )
    )

    log.info(
      "Date change flags: LSD ${sentenceChanges.lsdChanged} LED ${sentenceChanges.ledChanged} " +
        "SED ${sentenceChanges.sedChanged} TUSSD ${sentenceChanges.tussdChanged} TUSED ${sentenceChanges.tusedChanged} " +
        "isMaterial ${sentenceChanges.isMaterial}"
    )

    relevantListeners.forEach {
      it.afterFlush(licenceEntity, sentenceChanges)
    }

    if (!sentenceChanges.isMaterial) return

    // Notify the COM of any change to material dates on the licence
    updatedLicenceEntity.bookingId?.let {
      prisonApiClient.hdcStatus(it)
        .defaultIfEmpty(PrisonerHdcStatus(passed = false, approvalStatus = "UNKNOWN"))
        .filter { h -> h.approvalStatus != "APPROVED" }.subscribe {
          log.info("Notifying COM ${licenceEntity.responsibleCom?.email} of date change event for $licenceId")
          notifyService.sendDatesChangedEmail(
            licenceId.toString(),
            licenceEntity.responsibleCom?.email,
            "${licenceEntity.responsibleCom?.firstName} ${licenceEntity.responsibleCom?.lastName}",
            "${licenceEntity.forename} ${licenceEntity.surname}",
            licenceEntity.crn,
            mapOf(
              "Release date" to sentenceChanges.lsdChanged,
              "Release date" to sentenceChanges.lsdChanged,
              "Licence end date" to sentenceChanges.ledChanged,
              "Sentence end date" to sentenceChanges.sedChanged,
              "Top up supervision start date" to sentenceChanges.tussdChanged,
              "Top up supervision end date" to sentenceChanges.tusedChanged,
            ),
          )
        }
    }
  }

  private fun hasOffenderBeenReSentencedWithActiveLicence(
    sentenceDatesRequest: UpdateSentenceDatesRequest,
    licenceEntity: Licence
  ): Boolean {
    if (licenceEntity.statusCode == ACTIVE &&
      (
        sentenceDatesRequest.actualReleaseDate?.isAfter(LocalDate.now()) == true ||
          sentenceDatesRequest.conditionalReleaseDate?.isAfter(LocalDate.now()) == true
        )
    ) {
      log.warn("Active Licence ${licenceEntity.id} is no longer valid with ARD ${sentenceDatesRequest.actualReleaseDate} and CRD ${sentenceDatesRequest.conditionalReleaseDate}")
      return true
    }
    return false
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}