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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NOW_IN_HARDSTOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NO_CHANGE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NO_LONGER_IN_HARDSTOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.reifySentenceDates
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT

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

  @Transactional
  fun updateSentenceDates(licenceId: Long) {
    val licenceEntity = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val prisoner = prisonApiClient.getPrisonerDetail(licenceEntity.nomsId!!)
    val prisonerSearchPrisoner = prisoner.toPrisonerSearchPrisoner()
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisonerSearchPrisoner, licenceEntity.kind)

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val sentenceDates = prisoner.sentenceDetail.toSentenceDates()

    val sentenceChanges = licenceEntity.getDateChanges(sentenceDates, licenceStartDate)
    val dateChanges = sentenceChanges.dates.filter { !it.type.hdcOnly || licenceEntity is HdcLicence }

    logUpdate(licenceId, sentenceChanges.isMaterial, dateChanges)

    val previousSentenceDates = licenceEntity.reifySentenceDates()
    licenceEntity.updateLicenceDates(
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

    val hardstopChangeType = getHardstopChangeType(previousSentenceDates, licenceEntity)

    if (hardstopChangeType == NOW_IN_HARDSTOP) {
      licenceService.timeout(licenceEntity as CrdLicence, reason = "due to sentence dates update")
    } else {
      licenceRepository.saveAndFlush(licenceEntity)
      recordAuditEvent(licenceEntity, sentenceChanges)
    }

    if (hardstopChangeType == NO_LONGER_IN_HARDSTOP) {
      val licences = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        licenceEntity?.bookingId!!,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(CRD, HARD_STOP),
      )
      licenceService.inactivateLicences(licences, LICENCE_DEACTIVATION_HARD_STOP)
    }

    if (sentenceChanges.isMaterial) {
      val isNotApprovedForHdc = !hdcService.isApprovedForHdc(
        licenceEntity.bookingId!!,
        sentenceDates.homeDetentionCurfewEligibilityDate,
      )
      notifyComOfUpdate(licenceEntity, licenceId, dateChanges, isNotApprovedForHdc)
    }
  }

  private fun notifyComOfUpdate(
    licenceEntity: Licence,
    licenceId: Long,
    dateChanges: List<DateChange>,
    isNotApprovedForHdc: Boolean,
  ) {
    val notifyCom = licenceEntity is HdcLicence || isNotApprovedForHdc
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
      dateChanges.filter { it.changed && it.type.notifyOnChange }.map { it.toDescription() },
    )
  }

  private fun logUpdate(licenceId: Long, isMaterial: Boolean, dateChanges: List<DateChange>) {
    log.info(
      buildString {
        append("Licence dates - ID $licenceId ")
        dateChanges.forEach { append("${it.type.name} ${it.oldDate} ") }
      },
    )
    log.info(
      buildString {
        append("Event dates - ID $licenceId ")
        dateChanges.forEach { append("${it.type.name} ${it.newDate} ") }
      },
    )
    log.info(
      buildString {
        append("Date change flags: ")
        dateChanges.forEach { append("${it.type.name} ${it.changed} ") }
        append("isMaterial $isMaterial")
      },
    )
  }

  private fun recordAuditEvent(licenceEntity: Licence, sentenceChanges: SentenceChanges) {
    with(licenceEntity) {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = this.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Sentence dates updated for ${this.forename} ${this.surname}",
          detail = "ID ${this.id} type ${this.typeCode} status ${this.statusCode} version ${this.version}",
          changes = sentenceChanges.toChanges(licenceEntity.kind),
        ),
      )
    }
  }

  private fun getHardstopChangeType(previous: SentenceDateHolder, new: Licence): HardstopChangeType {
    val previouslyInHardstop = releaseDateService.isInHardStopPeriod(previous)
    val nowInHardstop = releaseDateService.isInHardStopPeriod(new)
    val isCrdInProgress = new is CrdLicence && new.statusCode == IN_PROGRESS
    return when {
      isCrdInProgress && !previouslyInHardstop && nowInHardstop -> NOW_IN_HARDSTOP
      previouslyInHardstop && !nowInHardstop -> NO_LONGER_IN_HARDSTOP
      else -> NO_CHANGE
    }
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
    const val LICENCE_DEACTIVATION_HARD_STOP =
      "Licence automatically inactivated as licence is no longer in hard stop period"
  }

  private enum class HardstopChangeType {
    NOW_IN_HARDSTOP,
    NO_LONGER_IN_HARDSTOP,
    NO_CHANGE,
  }
}
