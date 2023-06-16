package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime

@Service
class UpdateSentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
) {

  @Transactional
  fun updateSentenceDates(licenceId: Long, sentenceDatesRequest: UpdateSentenceDatesRequest) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    log.info(
      buildString {
        append("Licence dates - ID $licenceId ")
        append("CRD ${licenceEntity?.conditionalReleaseDate} ")
        append("ARD ${licenceEntity?.actualReleaseDate} ")
        append("SSD ${licenceEntity?.sentenceStartDate} ")
        append("SED ${licenceEntity?.sentenceEndDate} ")
        append("LSD ${licenceEntity?.licenceStartDate} ")
        append("LED ${licenceEntity?.licenceExpiryDate} ")
        append("TUSSD ${licenceEntity?.topupSupervisionStartDate} ")
        append("TUSED ${licenceEntity?.topupSupervisionExpiryDate}")
      },
    )

    log.info(
      buildString {
        append("Event dates - ID $licenceId ")
        append("CRD ${sentenceDatesRequest.conditionalReleaseDate} ")
        append("ARD ${sentenceDatesRequest.actualReleaseDate} ")
        append("SSD ${sentenceDatesRequest.sentenceStartDate} ")
        append("SED ${sentenceDatesRequest.sentenceEndDate} ")
        append("LSD ${sentenceDatesRequest.licenceStartDate} ")
        append("LED ${sentenceDatesRequest.licenceExpiryDate} ")
        append("TUSSD ${sentenceDatesRequest.topupSupervisionStartDate} ")
        append("TUSED ${sentenceDatesRequest.topupSupervisionExpiryDate}")
      },
    )

    val sentenceChanges = licenceEntity.getSentenceChanges(sentenceDatesRequest)

    val updatedLicenceEntity = licenceEntity.copy(
      statusCode = licenceEntity.calculateStatusCode(sentenceDatesRequest),
      conditionalReleaseDate = sentenceDatesRequest.conditionalReleaseDate,
      actualReleaseDate = sentenceDatesRequest.actualReleaseDate,
      sentenceStartDate = sentenceDatesRequest.sentenceStartDate,
      sentenceEndDate = sentenceDatesRequest.sentenceEndDate,
      licenceStartDate = sentenceDatesRequest.licenceStartDate,
      licenceExpiryDate = sentenceDatesRequest.licenceExpiryDate,
      topupSupervisionStartDate = sentenceDatesRequest.topupSupervisionStartDate,
      topupSupervisionExpiryDate = sentenceDatesRequest.topupSupervisionExpiryDate,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicenceEntity)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceEntity.id,
        username = "SYSTEM",
        fullName = "SYSTEM",
        eventType = AuditEventType.SYSTEM_EVENT,
        summary = "Sentence dates updated for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode} version ${licenceEntity.version}",
      ),
    )

    log.info(
      buildString {
        append("Date change flags: ")
        append("LSD ${sentenceChanges.lsdChanged} ")
        append("LED ${sentenceChanges.ledChanged} ")
        append("SED ${sentenceChanges.sedChanged} ")
        append("TUSSD ${sentenceChanges.tussdChanged} ")
        append("TUSED ${sentenceChanges.tusedChanged} ")
        append("isMaterial ${sentenceChanges.isMaterial}")
      },
    )

    if (!sentenceChanges.isMaterial) return

    // Notify the COM of any change to material dates on the licence
    updatedLicenceEntity.bookingId?.let {
      prisonApiClient.hdcStatus(it).defaultIfEmpty(PrisonerHdcStatus(passed = false, approvalStatus = "UNKNOWN"))
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
