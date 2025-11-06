package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AlwaysHasCom
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SentenceDateHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.SupportsHardStop
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.TIMED_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.TimeServedConsiderations

@Service
class UpdateSentenceDateService(
  private val licenceRepository: LicenceRepository,
  private val auditService: AuditService,
  private val notifyService: NotifyService,
  private val prisonApiClient: PrisonApiClient,
  private val hdcService: HdcService,
  private val staffRepository: StaffRepository,
  private val releaseDateService: ReleaseDateService,
  private val licenceService: LicenceService,
  private val cvlRecordService: CvlRecordService,
) {

  @Transactional
  fun updateSentenceDates(licenceId: Long) {
    val currentLicence = licenceRepository.findById(licenceId).orElseThrow { EntityNotFoundException("$licenceId") }
    val prisoner = prisonApiClient.getPrisonerDetail(currentLicence.nomsId!!)
    val prisonerSearchPrisoner = prisoner.toPrisonerSearchPrisoner()
    val cvlRecord = cvlRecordService.getCvlRecord(prisonerSearchPrisoner, currentLicence.probationAreaCode!!)

    val updatedLicence = licenceService.updateLicenceKind(currentLicence, cvlRecord.eligibleKind)
    val kindForLsdCalculations = selectCorrectKindForHardStopIfNeeded(updatedLicence, cvlRecord)
    val licenceStartDate = releaseDateService.getLicenceStartDate(prisonerSearchPrisoner, kindForLsdCalculations)

    val sentenceDates = prisoner.sentenceDetail.toSentenceDates()

    val dateChanges = currentLicence.getDateChanges(sentenceDates, licenceStartDate)

    logUpdate(
      currentLicence,
      dateChanges.isMaterial,
      dateChanges.filter { !it.type.hdcOnly || updatedLicence is HdcLicence },
    )

    val previousSentenceDates = currentLicence.reifySentenceDates()
    val user = staffRepository.findByUsernameIgnoreCase(SecurityContextHolder.getContext().authentication.name)
    updatedLicence.updateLicenceDates(
      status = updatedLicence.calculateStatusCode(sentenceDates),
      staffMember = user,
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

    val hardstopChangeType = getHardstopChangeType(previousSentenceDates, updatedLicence)

    if (hardstopChangeType == NOW_IN_HARDSTOP) {
      licenceService.timeout(updatedLicence, reason = "due to sentence dates update")
    } else {
      licenceRepository.saveAndFlush(updatedLicence)
      recordAuditEvent(updatedLicence, dateChanges)
    }

    if (hardstopChangeType == NO_LONGER_IN_HARDSTOP) {
      val licences = licenceRepository.findAllByBookingIdAndStatusCodeInAndKindIn(
        updatedLicence.bookingId!!,
        listOf(IN_PROGRESS, SUBMITTED, APPROVED, TIMED_OUT),
        listOf(CRD, HARD_STOP),
      )
      licenceService.inactivateLicences(licences, LICENCE_DEACTIVATION_HARD_STOP)
    }

    if (dateChanges.isMaterial) {
      val isNotApprovedForHdc = !hdcService.isApprovedForHdc(
        updatedLicence.bookingId!!,
        sentenceDates.homeDetentionCurfewEligibilityDate,
      )
      notifyComOfUpdate(updatedLicence, dateChanges, isNotApprovedForHdc)
    }
  }

  /*
   * This code is to make sure that hard stop licences do not default to CRD LSD Calculations in getLicenceStartDate
   * and if they are PRRD the LSD is calculated appropriately
   */
  private fun selectCorrectKindForHardStopIfNeeded(
    licence: Licence,
    cvlRecord: CvlRecord,
  ): LicenceKind? = if (licence.kind == HARD_STOP) {
    cvlRecord.eligibleKind
  } else {
    licence.kind
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
    log.info("Notifying COM ${licence.responsibleCom?.email} of date change event for ${licence.id}")

    notifyService.sendDatesChangedEmail(
      licence.id.toString(),
      licence.getCom().email,
      licence.getCom().fullName,
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

    if (licence.kind == PRRD) {
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
      auditService.recordAuditEvent(
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
    val previouslyInHardstop = releaseDateService.isInHardStopPeriod(previous.licenceStartDate)
    val nowInHardstop = releaseDateService.isInHardStopPeriod(new.licenceStartDate)
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
