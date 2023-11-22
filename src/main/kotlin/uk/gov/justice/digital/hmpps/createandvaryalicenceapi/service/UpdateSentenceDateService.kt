package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class UpdateSentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
) {
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

  @Transactional
  fun updateSentenceDates(licenceId: Long, sentenceDatesRequest: UpdateSentenceDatesRequest) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    logUpdate(licenceId, licenceEntity, sentenceDatesRequest)

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
      postRecallReleaseDate = sentenceDatesRequest.postRecallReleaseDate,
      paroleEligibilityDate = sentenceDatesRequest.paroleEligibilityDate,
      nonParoleDate = sentenceDatesRequest.nonParoleDate,
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
        append("PRRD ${sentenceChanges.prrdChanged} ")
        append("PED ${sentenceChanges.pedChanged} ")
        append("NPD ${sentenceChanges.npdChanged} ")
        append("isMaterial ${sentenceChanges.isMaterial}")
      },
    )

    if (sentenceChanges.isMaterial) {
      notifyComOfUpdate(updatedLicenceEntity, licenceEntity, licenceId, sentenceChanges)
    }
  }

  private fun notifyComOfUpdate(
    updatedLicenceEntity: Licence,
    licenceEntity: Licence,
    licenceId: Long,
    sentenceChanges: SentenceChanges,
  ) {
    updatedLicenceEntity.bookingId?.let {
      prisonApiClient.getHdcStatus(it).defaultIfEmpty(PrisonerHdcStatus(passed = false, approvalStatus = "UNKNOWN"))
        .filter { h -> h.approvalStatus != "APPROVED" }.subscribe {
          log.info("Notifying COM ${licenceEntity.responsibleCom?.email} of date change event for $licenceId")
          notifyService.sendDatesChangedEmail(
            licenceId.toString(),
            licenceEntity.responsibleCom?.email,
            "${licenceEntity.responsibleCom?.firstName} ${licenceEntity.responsibleCom?.lastName}",
            "${licenceEntity.forename} ${licenceEntity.surname}",
            licenceEntity.crn,
            mapOf(
              "Release date has changed to ${updatedLicenceEntity.licenceStartDate?.format(dateFormat)}" to sentenceChanges.lsdChanged,
              "Licence end date has changed to ${updatedLicenceEntity.licenceExpiryDate?.format(dateFormat)}" to sentenceChanges.ledChanged,
              "Sentence end date has changed to ${updatedLicenceEntity.sentenceEndDate?.format(dateFormat)}" to sentenceChanges.sedChanged,
              "Top up supervision start date has changed to ${
                updatedLicenceEntity.topupSupervisionStartDate?.format(
                  dateFormat,
                )
              }" to sentenceChanges.tussdChanged,
              "Top up supervision end date has changed to ${
                updatedLicenceEntity.topupSupervisionExpiryDate?.format(
                  dateFormat,
                )
              }" to sentenceChanges.tusedChanged,
              "Post recall release date has changed to ${updatedLicenceEntity.postRecallReleaseDate?.format(dateFormat)}" to sentenceChanges.prrdChanged,
              "Parole eligibility date has changed to ${updatedLicenceEntity.paroleEligibilityDate?.format(dateFormat)}" to sentenceChanges.pedChanged,
              "Non parole date has changed to ${updatedLicenceEntity.nonParoleDate?.format(dateFormat)}" to sentenceChanges.npdChanged,
            ),
          )
        }
    }
  }

  private fun logUpdate(
    licenceId: Long,
    licenceEntity: Licence?,
    sentenceDatesRequest: UpdateSentenceDatesRequest,
  ) {
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
        append("PED ${licenceEntity?.paroleEligibilityDate}")
        append("NPD ${licenceEntity?.nonParoleDate}")
        append("PRRD ${licenceEntity?.postRecallReleaseDate}")
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
        append("PED ${sentenceDatesRequest.paroleEligibilityDate}")
        append("NPD ${sentenceDatesRequest.nonParoleDate}")
        append("PRRD ${sentenceDatesRequest.postRecallReleaseDate}")
      },
    )
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}
