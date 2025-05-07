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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Service
class UpdateSentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditEventRepository: AuditEventRepository,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
  private val hdcService: HdcService,
  private val staffRepository: StaffRepository,
  private val releaseDateService: ReleaseDateService,
  private val licenceService: LicenceService,
) {
  val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd LLLL yyyy")

  @Transactional
  fun updateSentenceDates(licenceId: Long) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val prisoner = prisonApiClient.getPrisonerDetail(licenceEntity.nomsId!!)
    val prisonerSearchPrisoner = prisoner.toPrisonerSearchPrisoner()
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisonerSearchPrisoner, licenceEntity.kind)

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val sentenceDates = prisoner.sentenceDetail.toSentenceDates()

    logUpdate(licenceId, licenceEntity, licenceStartDate, sentenceDates)

    val sentenceChanges = licenceEntity.getSentenceChanges(sentenceDates, licenceStartDate)

    val updatedLicenceEntity = licenceEntity.updateLicenceDates(
      status = licenceEntity.calculateStatusCode(sentenceDates),
      staffMember = staffMember,
      licenceStartDate = licenceStartDate,
      conditionalReleaseDate = sentenceDates.conditionalReleaseDate,
      actualReleaseDate = sentenceDates.actualReleaseDate,
      sentenceStartDate = sentenceDates.sentenceStartDate,
      sentenceEndDate = sentenceDates.sentenceEndDate,
      licenceExpiryDate = sentenceDates.licenceExpiryDate,
      topupSupervisionStartDate = sentenceDates.topupSupervisionStartDate,
      topupSupervisionExpiryDate = sentenceDates.topupSupervisionExpiryDate,
      postRecallReleaseDate = sentenceDates.postRecallReleaseDate,
      homeDetentionCurfewActualDate = sentenceDates.homeDetentionCurfewActualDate,
      homeDetentionCurfewEndDate = sentenceDates.homeDetentionCurfewEndDate,
    )

    val licencePreviouslyInHardStopPeriod = releaseDateService.isInHardStopPeriod(licenceEntity)
    val licenceCurrentlyInHardStopPeriod = releaseDateService.isInHardStopPeriod(updatedLicenceEntity)
    val isLicenceStatusInProgress = updatedLicenceEntity.statusCode == IN_PROGRESS
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
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(CRD, HARD_STOP),
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
          append("HDCENDDATE ${sentenceChanges.hdcEndDateChanged} ")
        }
        append("isMaterial ${sentenceChanges.isMaterial}")
      },
    )

    if (sentenceChanges.isMaterial) {
      val isNotApprovedForHdc = !hdcService.isApprovedForHdc(
        updatedLicenceEntity.bookingId!!,
        sentenceDates.homeDetentionCurfewEligibilityDate,
      )
      notifyComOfUpdate(updatedLicenceEntity, licenceEntity, licenceId, sentenceChanges, isNotApprovedForHdc)
    }
  }

  private fun notifyComOfUpdate(
    updatedLicenceEntity: Licence,
    licenceEntity: Licence,
    licenceId: Long,
    sentenceChanges: SentenceChanges,
    isNotApprovedForHdc: Boolean,
  ) {
    val notifyCom = updatedLicenceEntity is HdcLicence || isNotApprovedForHdc
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

  private fun logUpdate(
    licenceId: Long,
    licenceEntity: Licence?,
    licenceStartDate: LocalDate?,
    sentenceDates: SentenceDates,
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
        append("CRD ${sentenceDates.conditionalReleaseDate} ")
        append("ARD ${sentenceDates.actualReleaseDate} ")
        append("SSD ${sentenceDates.sentenceStartDate} ")
        append("SED ${sentenceDates.sentenceEndDate} ")
        append("LSD $licenceStartDate ")
        append("LED ${sentenceDates.licenceExpiryDate} ")
        append("TUSSD ${sentenceDates.topupSupervisionStartDate} ")
        append("TUSED ${sentenceDates.topupSupervisionExpiryDate} ")
        append("PRRD ${sentenceDates.postRecallReleaseDate}")
        if (licenceEntity is HdcLicence) {
          append("HDCAD ${sentenceDates.homeDetentionCurfewActualDate} ")
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
