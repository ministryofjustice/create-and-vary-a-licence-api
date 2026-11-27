package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.ADDRESS_WITHDRAWN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.POSTPONE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.REFUSE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.CurfewTimesMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType.SYSTEM_EVENT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.CRD_CREATED_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType.INACTIVE_WHEN_HDC_REFUSED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDateTime

@Service
class HdcEventService(
  private val licenceRepository: LicenceRepository,
  private val hdcLicenceRepository: HdcLicenceRepository,
  private val auditService: AuditService,
  private val crdLicenceFactory: CrdLicenceFactory,
  private val hdcLicenceFactory: HdcLicenceFactory,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val linkingService: LicenceLinkingService,
  private val licenceService: LicenceService,
  private val hdcLifecycleService: HdcLifecycleService,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  private fun getHdcLicencesByStatus(nomsNumber: String) = hdcLicenceRepository
    .findInFlightHdcLicences(nomsNumber)
    .associateBy { it.statusCode }

  private fun <T> getLicenceToConvert(licencesByStatus: Map<LicenceStatus, T>): T = licencesByStatus[IN_PROGRESS]
    ?: licencesByStatus[SUBMITTED]
    ?: requireNotNull(licencesByStatus[APPROVED])

  private fun inactivateConversionLicence(
    eventType: HdcCvlEventType,
    licencesByStatus: Map<LicenceStatus, Licence>,
  ) {
    val reason = "Licence automatically inactivated after HDC ${eventType.name} event"
    licenceService.inactivateLicences(
      licencesByStatus.values.toList(),
      reason = reason,
      deactivateInProgressVersions = true,
    )
  }

  private fun addConversionAudits(parent: Licence, newLicence: Licence, eventType: HdcCvlEventType) {
    val detail = "Old ID ${parent.id}, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}"
    auditService.recordAuditEvent(
      AuditEvent(
        licenceId = parent.id,
        summary = "${parent.kind} licence converted to ${newLicence.kind} licence on ${eventType.name}",
        detail = detail,
        eventType = SYSTEM_EVENT,
      ),
    )
    auditService.recordAuditEvent(
      AuditEvent(
        licenceId = newLicence.id,
        summary = "${newLicence.kind} licence converted from ${parent.kind} on ${eventType.name}",
        detail = detail,
        eventType = SYSTEM_EVENT,
      ),
    )
  }

  private fun addLicenceConversionEvents(parent: Licence, newLicence: Licence, eventType: HdcCvlEventType) {
    val (inactiveEventType, createdEventType) = when (eventType) {
      OPT_IN -> Pair(INACTIVE_WHEN_HDC_OPT_IN, CRD_CREATED_WHEN_HDC_OPT_IN)
      OPT_OUT -> Pair(INACTIVE_WHEN_HDC_OPT_OUT, CRD_CREATED_WHEN_HDC_OPT_OUT)
      REFUSE -> Pair(INACTIVE_WHEN_HDC_REFUSED, CRD_CREATED_WHEN_HDC_REFUSED)
      else -> throw IllegalArgumentException("Unknown HDC conversion event type $eventType")
    }

    licenceService.createLicenceEvent(
      licenceId = parent.id,
      eventType = inactiveEventType,
      eventDescription = "This ${parent.kind} licence was converted to ${newLicence.kind} licence on ${eventType.name}",
    )
    licenceService.createLicenceEvent(
      licenceId = newLicence.id,
      eventType = createdEventType,
      eventDescription = "This ${newLicence.kind} licence was converted from ${parent.kind} licence on ${eventType.name}",
    )
  }

  private fun addLicenceLinking(eventType: HdcCvlEventType, licenceToConvert: Licence, newLicence: Licence) {
    when (eventType) {
      OPT_IN -> linkingService.setHdcReplacementForOptedInLicence(licenceToConvert.id, newLicence.id)
      OPT_OUT -> linkingService.setCrdReplacementForOptedOutLicence(licenceToConvert.id, newLicence.id)
      REFUSE -> linkingService.setCrdReplacementForRefusedLicence(licenceToConvert.id, newLicence.id)
      else -> throw IllegalArgumentException("Unknown HDC conversion event type $eventType")
    }
  }

  private fun getCrdLicencesByStatus(nomsNumber: String) = licenceRepository
    .findInFlightCrdLicences(nomsNumber)
    .associateBy { it.statusCode }

  private fun updatePostponement(licence: HdcLicence, isPostponed: Boolean): Boolean {
    if (licence.application?.isPostponed == isPostponed) return false

    val operation = if (isPostponed) "Postponing" else "Resuming"
    log.info("{} HDC licence, id: '{}', status: '{}', for prisoner: '{}'", operation, licence.id, licence.statusCode, licence.nomsId)
    val application = requireNotNull(licence.application) {
      "HDC application not found for licence ${licence.id}"
    }
    application.isPostponed = isPostponed
    application.dateLastUpdated = LocalDateTime.now()
    auditService.recordAuditEventHdcLicencePostponementChange(licence, isPostponed)
    return true
  }

  @Transactional
  fun convertToCrdLicence(nomsNumber: String, eventType: HdcCvlEventType) {
    val licencesByStatus = getHdcLicencesByStatus(nomsNumber)
    if (licencesByStatus.isEmpty()) return

    val licenceToConvert = getLicenceToConvert(licencesByStatus)
    inactivateConversionLicence(eventType, licencesByStatus)
    val crdCopy = crdLicenceFactory.createFromHdc(licenceToConvert, IN_PROGRESS)
    val newLicence = licenceService.populateCopy(original = licenceToConvert, copy = crdCopy)
    addConversionAudits(licenceToConvert, newLicence, eventType)
    addLicenceConversionEvents(licenceToConvert, newLicence, eventType)
    addLicenceLinking(eventType, licenceToConvert, newLicence)
  }

  @Transactional
  fun convertToHdcLicence(nomsNumber: String) {
    val licencesByStatus = getCrdLicencesByStatus(nomsNumber)
    if (licencesByStatus.isEmpty()) return

    val licenceToConvert = getLicenceToConvert(licencesByStatus)
    inactivateConversionLicence(OPT_IN, licencesByStatus)
    val prisoner = prisonerSearchApiClient.searchPrisonersByNomisIds(listOf(nomsNumber)).firstOrNull()
    val hdcCopy = hdcLicenceFactory.createFromCrd(licenceToConvert, prisoner, IN_PROGRESS)
    addLastHdcLicenceDetails(licenceToConvert, hdcCopy)
    val newLicence = licenceService.populateCopy(original = licenceToConvert, copy = hdcCopy) as HdcLicence
    addConversionAudits(licenceToConvert, newLicence, OPT_IN)
    addLicenceConversionEvents(licenceToConvert, newLicence, OPT_IN)
    addLicenceLinking(OPT_IN, licenceToConvert, newLicence)

    hdcLifecycleService.populateOnCreation(newLicence)
  }

  private fun addLastHdcLicenceDetails(
    licenceToConvert: CrdLicence,
    newLicence: HdcLicence,
  ) {
    val previousHdcLicenceId = linkingService.getHdcLicenceIdForHdcReplacement(licenceToConvert.id)
      ?: linkingService.getHdcLicenceIdForCrdReplacement(licenceToConvert.id)
    previousHdcLicenceId?.let { licenceId ->
      hdcLicenceRepository.findByIdOrNull(licenceId)
    }?.let {
      newLicence.weeklyCurfewTimes = CurfewTimesMapper.copyList(it.weeklyCurfewTimes)
      newLicence.firstNightCurfewTimes = CurfewTimesMapper.copy(it.firstNightCurfewTimes)
    }
  }

  @Transactional
  fun postponeLicence(nomsNumber: String) {
    val licencesByStatus = getHdcLicencesByStatus(nomsNumber)
    if (licencesByStatus.isEmpty()) {
      log.info("HDC postpone: no eligible HDC licence found for nomsNumber={}", nomsNumber)
      return
    }

    if (licencesByStatus.values.count { updatePostponement(it, isPostponed = true) } == 0) return
    val approvedLicence = licencesByStatus[APPROVED]
    val editableLicence = licencesByStatus[SUBMITTED] ?: licencesByStatus[IN_PROGRESS]
    approvedLicence?.let { approved ->
      if (editableLicence == null) {
        revertLicenceToInProgress(approved, POSTPONE)
      } else {
        licenceService.inactivateLicences(
          listOf(approved),
          reason = "HDC licence postponed",
          deactivateInProgressVersions = false,
        )
      }
    }
    editableLicence?.takeIf { it.statusCode == SUBMITTED }?.let { revertLicenceToInProgress(it, POSTPONE) }
  }

  @Transactional
  fun resumeHdcPostponement(nomsNumber: String) {
    val licencesByStatus = getHdcLicencesByStatus(nomsNumber)
    if (licencesByStatus.isEmpty()) {
      log.warn("HDC resume: no eligible HDC licence found for nomsNumber={}", nomsNumber)
      return
    }

    if (licencesByStatus.values.count { updatePostponement(it, isPostponed = false) } == 0) return
    log.info("HDC resume: cleared postponement state for nomsNumber={}", nomsNumber)
  }

  @Transactional
  fun withdrawAddress(nomsNumber: String) {
    val licencesByStatus = getHdcLicencesByStatus(nomsNumber)
    if (licencesByStatus.isEmpty()) {
      log.warn("HDC address withdrawn: no eligible licence found for nomsNumber={}", nomsNumber)
      return
    }

    hdcLifecycleService.withdrawHdcCurfewAddresses(licencesByStatus.values.toList())
    val approvedLicence = licencesByStatus[APPROVED]
    val editableLicence = licencesByStatus[SUBMITTED] ?: licencesByStatus[IN_PROGRESS]
    approvedLicence?.let { approved ->
      if (editableLicence == null) {
        revertLicenceToInProgress(approved, ADDRESS_WITHDRAWN)
      } else {
        licenceService.inactivateLicences(
          listOf(approved),
          reason = "HDC address withdrawn",
          deactivateInProgressVersions = false,
        )
      }
    }
    editableLicence?.takeIf { it.statusCode == SUBMITTED }?.let { revertLicenceToInProgress(it, ADDRESS_WITHDRAWN) }
  }

  internal fun revertLicenceToInProgress(licence: HdcLicence, eventType: HdcCvlEventType) {
    val recordAuditEvent = when (eventType) {
      ADDRESS_WITHDRAWN -> auditService::recordAuditEventHdcLicenceAddressWithdrawn
      POSTPONE -> auditService::recordAuditEventHdcLicencePostponed
      else -> throw IllegalArgumentException("Unknown HDC revert event type $eventType")
    }

    if (licence.statusCode == IN_PROGRESS) return
    val previousStatus = licence.statusCode
    licence.statusCode = IN_PROGRESS
    licence.approvedByUsername = null
    licence.approvedByName = null
    licence.approvedDate = null
    licence.supersededDate = null

    recordAuditEvent(licence, previousStatus)
  }
}
