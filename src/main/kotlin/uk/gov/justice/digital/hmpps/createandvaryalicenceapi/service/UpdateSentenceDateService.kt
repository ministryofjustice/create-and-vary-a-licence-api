package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.format.DateTimeFormatter

@Service
class UpdateSentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
  private val staffRepository: StaffRepository,
  private val releaseDateService: ReleaseDateService,
  private val licenceService: LicenceService,
) {
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

  @Transactional
  fun updateSentenceDates(licenceId: Long, sentenceDatesRequest: UpdateSentenceDatesRequest) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val prisoner = prisonApiClient.getPrisonerDetail(licenceEntity.nomsId!!)
    val prisonerSearchPrisoner = prisoner.toPrisonerSearchPrisoner()
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisonerSearchPrisoner, licenceEntity.kind)

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    logUpdate(licenceId, licenceEntity, sentenceDatesRequest)

    val sentenceChanges = licenceEntity.getSentenceChanges(sentenceDatesRequest, licenceStartDate)

    val updatedLicenceEntity = licenceEntity.updateLicenceDates(
      status = licenceEntity.calculateStatusCode(sentenceDatesRequest),
      conditionalReleaseDate = sentenceDatesRequest.conditionalReleaseDate,
      actualReleaseDate = sentenceDatesRequest.actualReleaseDate,
      sentenceStartDate = sentenceDatesRequest.sentenceStartDate,
      sentenceEndDate = sentenceDatesRequest.sentenceEndDate,
      licenceStartDate = licenceStartDate,
      licenceExpiryDate = sentenceDatesRequest.licenceExpiryDate,
      topupSupervisionStartDate = sentenceDatesRequest.topupSupervisionStartDate,
      topupSupervisionExpiryDate = sentenceDatesRequest.topupSupervisionExpiryDate,
      postRecallReleaseDate = sentenceDatesRequest.postRecallReleaseDate,
      homeDetentionCurfewActualDate = sentenceDatesRequest.homeDetentionCurfewActualDate,
      homeDetentionCurfewEndDate = sentenceDatesRequest.homeDetentionCurfewEndDate,
      staffMember = staffMember,
    )

    val licencePreviouslyInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceEntity)
    val licenceCurrentlyInHardStopPeriod = releaseDateService.isInHardStopPeriod(updatedLicenceEntity)
    val isLicenceStatusInProgress = updatedLicenceEntity.statusCode == LicenceStatus.IN_PROGRESS
    val isTimedOut =
      !licencePreviouslyInHardStopPeriod && licenceCurrentlyInHardStopPeriod && isLicenceStatusInProgress

    if (isTimedOut && updatedLicenceEntity is CrdLicence) {
      licenceService.timeout(updatedLicenceEntity, reason = "due to sentence dates update")
    } else {
      licenceRepository.saveAndFlush(updatedLicenceEntity)
      recordAuditEvent(updatedLicenceEntity, "Sentence dates updated")
    }

    if (licencePreviouslyInHardStopPeriod && !licenceCurrentlyInHardStopPeriod) {
      val licences = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        licenceEntity?.bookingId!!,
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.TIMED_OUT),
        listOf(LicenceKind.CRD, LicenceKind.HARD_STOP),
      )
      licenceService.inactivateLicences(licences, LICENCE_DEACTIVATION_HARD_STOP)
    }

    log.info(
      buildString {
        append("Date change flags: ")
        append("LSD ${sentenceChanges.lsdChanged} ")
        append("LED ${sentenceChanges.ledChanged} ")
        append("SED ${sentenceChanges.sedChanged} ")
        append("TUSSD ${sentenceChanges.tussdChanged} ")
        append("TUSED ${sentenceChanges.tusedChanged} ")
        append("PRRD ${sentenceChanges.prrdChanged} ")
        if (licenceEntity is HdcLicence) {
          append("HDCAD ${sentenceChanges.hdcadChanged} ")
          append("HDCAD ${sentenceChanges.hdcEndDateChanged} ")
        }
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
    val notifyCom = updatedLicenceEntity is HdcLicence || isNotApprovedForHdc(updatedLicenceEntity)
    if (!notifyCom) {
      log.info("Not notifying COM as now approved for HDC for $licenceId")
      return
    }
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
        "Post recall release date has changed to ${
          updatedLicenceEntity.postRecallReleaseDate?.format(
            dateFormat,
          )
        }" to sentenceChanges.prrdChanged,
        "HDC actual date has changed to ${
          if (updatedLicenceEntity is HdcLicence) {
            updatedLicenceEntity.homeDetentionCurfewActualDate?.format(
              dateFormat,
            )
          } else {
            null
          }
        }" to sentenceChanges.hdcadChanged,
        "HDC end date has changed to ${
          if (updatedLicenceEntity is HdcLicence) {
            updatedLicenceEntity.homeDetentionCurfewEndDate?.format(
              dateFormat,
            )
          } else {
            null
          }
        }" to sentenceChanges.hdcEndDateChanged,
      ),
    )
  }

  private fun isNotApprovedForHdc(updatedLicenceEntity: Licence): Boolean {
    val hdcStatus = prisonApiClient.getHdcStatus(updatedLicenceEntity.bookingId!!)
      .defaultIfEmpty(PrisonerHdcStatus(passed = false, approvalStatus = "UNKNOWN"))
      .block()!!
    return hdcStatus.approvalStatus != "APPROVED"
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
        append("TUSED ${licenceEntity?.topupSupervisionExpiryDate} ")
        append("PRRD ${licenceEntity?.postRecallReleaseDate}")
        if (licenceEntity is HdcLicence) {
          append("HDCAD ${licenceEntity.homeDetentionCurfewActualDate} ")
        }
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
        append("TUSED ${sentenceDatesRequest.topupSupervisionExpiryDate} ")
        append("PRRD ${sentenceDatesRequest.postRecallReleaseDate}")
        if (licenceEntity is HdcLicence) {
          append("HDCAD ${sentenceDatesRequest.homeDetentionCurfewActualDate} ")
        }
      },
    )
  }

  private fun recordAuditEvent(licenceEntity: Licence, auditEventDescription: String) {
    with(licenceEntity) {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = this.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "$auditEventDescription for ${this.forename} ${this.surname}",
          detail = "ID ${this.id} type ${this.typeCode} status ${this.statusCode} version ${this.version}",
        ),
      )
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val LICENCE_DEACTIVATION_HARD_STOP =
      "Licence automatically inactivated as licence is no longer in hard stop period"
  }
}
