package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AlwaysHasCom
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SupportsHardStop
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NOW_IN_HARDSTOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NO_CHANGE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService.HardstopChangeType.NO_LONGER_IN_HARDSTOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.DateChange
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.DateChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.getDateChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDateHolderAdapter.reifySentenceDates
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HARD_STOP
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations

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
    val licence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val prisoner = prisonApiClient.getPrisonerDetail(licence.nomsId!!)
    val prisonerSearchPrisoner = prisoner.toPrisonerSearchPrisoner()
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisonerSearchPrisoner, licence.kind)

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val sentenceDates = prisoner.sentenceDetail.toSentenceDates()

    val dateChanges = licence.getDateChanges(sentenceDates, licenceStartDate)

    logUpdate(
      licence,
      dateChanges.isMaterial,
      dateChanges.filter { !it.type.hdcOnly || licence is HdcLicence },
    )

    val previousSentenceDates = licence.reifySentenceDates()
    licence.updateLicenceDates(
      status = licence.calculateStatusCode(sentenceDates),
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

    val hardstopChangeType = getHardstopChangeType(previousSentenceDates, licence)

    if (hardstopChangeType == NOW_IN_HARDSTOP) {
      licenceService.timeout(licence, reason = "due to sentence dates update")
    } else {
      licenceRepository.saveAndFlush(licence)
      recordAuditEvent(licence, dateChanges)
    }

    if (hardstopChangeType == NO_LONGER_IN_HARDSTOP) {
      val licences = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        licence?.bookingId!!,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(CRD, HARD_STOP),
      )
      licenceService.inactivateLicences(licences, LICENCE_DEACTIVATION_HARD_STOP)
    }

    if (dateChanges.isMaterial) {
      val isNotApprovedForHdc = !hdcService.isApprovedForHdc(
        licence.bookingId!!,
        sentenceDates.homeDetentionCurfewEligibilityDate,
      )
      notifyComOfUpdate(licence, dateChanges, isNotApprovedForHdc)
    }
  }

  @TimeServedConsiderations("Notify COM of date change event for a licence, if a COM is not set, should this be a team email?")
  private fun notifyComOfUpdate(
    licence: Licence,
    dateChanges: DateChanges,
    isNotApprovedForHdc: Boolean,
  ) {
    check(licence is AlwaysHasCom) { "Licence ${licence.id} does not have a responsible COM to notify of an update" }
    val notifyCom = licence is HdcLicence || isNotApprovedForHdc
    if (!notifyCom) {
      log.info("Not notifying COM as now approved for HDC for ${licence.id}")
      return
    }
    log.info("Notifying COM ${licence.responsibleCom.email} of date change event for ${licence.id}")

    notifyService.sendDatesChangedEmail(
      licence.id.toString(),
      licence.responsibleCom.email,
      "${licence.responsibleCom.firstName} ${licence.responsibleCom.lastName}",
      "${licence.forename} ${licence.surname}",
      licence.crn,
      dateChanges.filter { it.notifyOfChange(licence.kind) }.map { it.toDescription() },
    )
  }

  private fun logUpdate(licence: Licence, isMaterial: Boolean, dateChanges: List<DateChange>) {
    log.info(
      buildString {
        append("Licence dates - ID ${licence.id} ")
        dateChanges.forEach { append("${it.type.name} ${it.oldDate} ") }
      },
    )
    log.info(
      buildString {
        append("Event dates - ID ${licence.id} ")
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

    if (licence.kind == LicenceKind.PRRD) {
      val prrdChange = dateChanges.firstOrNull { it.type == LicenceDateType.PRRD }
      if (prrdChange != null) {
        if (prrdChange.oldDate != null && prrdChange.newDate == null) {
          log.info("PRRD licence with id ${licence.id}, status ${licence.statusCode} has had a PRRD removed")
        }
        if (prrdChange.oldDate == null && prrdChange.newDate != null) {
          log.info("PRRD licence with id ${licence.id}, status ${licence.statusCode} has had a PRRD added")
        }
      }
    }
  }

  private fun recordAuditEvent(licenceEntity: Licence, dateChanges: DateChanges) {
    with(licenceEntity) {
      auditEventRepository.saveAndFlush(
        AuditEvent(
          licenceId = this.id,
          username = "SYSTEM",
          fullName = "SYSTEM",
          eventType = AuditEventType.SYSTEM_EVENT,
          summary = "Sentence dates updated for ${this.forename} ${this.surname}",
          detail = "ID ${this.id} type ${this.typeCode} status ${this.statusCode} version ${this.version}",
          changes = dateChanges.toChanges(licenceEntity.kind),
        ),
      )
    }
  }

  private fun getHardstopChangeType(previous: SentenceDateHolder, new: Licence): HardstopChangeType {
    val previouslyInHardstop = releaseDateService.isInHardStopPeriod(previous)
    val nowInHardstop = releaseDateService.isInHardStopPeriod(new)
    val isPotentialHardStopInProgress = new is SupportsHardStop && new.statusCode == IN_PROGRESS
    return when {
      isPotentialHardStopInProgress && !previouslyInHardstop && nowInHardstop -> NOW_IN_HARDSTOP
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
