package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcPostponement
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcPostponementRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.REFUSE
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
import kotlin.collections.isNotEmpty
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence as HdcLicenceEntity

@Service
class HdcLifecycleService(
  private val licenceRepository: LicenceRepository,
  private val hdcLicenceRepository: HdcLicenceRepository,
  private val hdcPostponementRepository: HdcPostponementRepository,
  private val auditService: AuditService,
  private val crdLicenceFactory: CrdLicenceFactory,
  private val hdcLicenceFactory: HdcLicenceFactory,
  private val linkingService: LicenceLinkingService,
) {

  @Lazy
  @Autowired
  lateinit var licenceService: LicenceService

  private val log = LoggerFactory.getLogger(this::class.java)

  private fun getHdcLicencesByInflightStatusMap(nomsNumber: String): Map<LicenceStatus, HdcLicenceEntity> {
    val licences = hdcLicenceRepository.findInFlightHdcLicences(nomsNumber)
    return licences.associateBy { it.statusCode }
  }

  private fun <T> getLicenceToConvert(
    licencesByStatus: Map<LicenceStatus, T>,
  ): T = licencesByStatus[IN_PROGRESS]
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

  private fun addConversionAudits(
    parent: Licence,
    newLicence: Licence,
    eventType: HdcCvlEventType,
  ) {
    val detail = "Old ID ${parent.id}, new ID ${newLicence.id} type ${newLicence.typeCode} status ${newLicence.statusCode.name} version ${newLicence.version}"
    val summaryParent = "${parent.kind} licence converted to ${newLicence.kind} licence on ${eventType.name}"

    val auditForParent = AuditEvent(
      licenceId = parent.id,
      summary = summaryParent,
      detail = detail,
      eventType = SYSTEM_EVENT,
    )
    auditService.recordAuditEvent(auditForParent)

    val summaryChild = "${newLicence.kind} licence converted from ${parent.kind} on ${eventType.name}"

    val auditForChild = AuditEvent(
      licenceId = newLicence.id,
      summary = summaryChild,
      detail = detail,
      eventType = SYSTEM_EVENT,
    )
    auditService.recordAuditEvent(auditForChild)
  }

  private fun addLicenceConversionEvents(
    parent: Licence,
    newLicence: Licence,
    eventType: HdcCvlEventType,
  ) {
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

  private fun addLicenceLinking(
    eventType: HdcCvlEventType,
    licenceToConvert: Licence,
    newLicence: Licence,
  ) {
    when (eventType) {
      OPT_IN -> linkingService.setHdcReplacementForOptedInLicence(licenceToConvert.id, newLicence.id)
      OPT_OUT -> linkingService.setCrdReplacementForOptedOutLicence(licenceToConvert.id, newLicence.id)
      REFUSE -> linkingService.setCrdReplacementForRefusedLicence(licenceToConvert.id, newLicence.id)
      else -> throw IllegalArgumentException("Unknown HDC conversion event type $eventType")
    }
  }

  private fun getCrdLicencesByInflightStatusMap(nomsNumber: String): Map<LicenceStatus, CrdLicence> {
    val licences = licenceRepository.findInFlightCrdLicences(nomsNumber)
    return licences.associateBy { it.statusCode }
  }

  private fun recordPostponement(licence: HdcLicenceEntity, isPostponed: Boolean) {
    val operation = if (isPostponed) "Postponing" else "Resuming"
    log.info("{} HDC licence, id: '{}', status: '{}', for prisoner: '{}'", operation, licence.id, licence.statusCode, licence.nomsId)
    val existing = hdcPostponementRepository.findByLicenceId(licence.id)

    if (existing != null) {
      existing.isPostponed = isPostponed
      existing.dateLastUpdated = LocalDateTime.now()
    } else {
      hdcPostponementRepository.save(
        HdcPostponement(
          licence = licence,
          isPostponed = isPostponed,
        ),
      )
    }

    auditService.recordAuditEventHdcLicencePostponementChange(licence, isPostponed)
  }

  @Transactional
  fun convertToCrdLicence(nomsNumber: String, eventType: HdcCvlEventType) {
    val licencesByStatus = getHdcLicencesByInflightStatusMap(nomsNumber)
    if (licencesByStatus.isNotEmpty()) {
      val licenceToConvert = getLicenceToConvert(licencesByStatus)
      inactivateConversionLicence(eventType, licencesByStatus)
      val crdCopy = crdLicenceFactory.createFromHdc(licenceToConvert, IN_PROGRESS)

      val newLicence = licenceService.populateCopy(
        original = licenceToConvert,
        copy = crdCopy,
      )

      addConversionAudits(licenceToConvert, newLicence, eventType)
      addLicenceConversionEvents(licenceToConvert, newLicence, eventType)
      addLicenceLinking(eventType, licenceToConvert, newLicence)
    }
  }

  @Transactional
  fun convertToHdcLicence(nomsNumber: String) {
    val licencesByStatus = getCrdLicencesByInflightStatusMap(nomsNumber)
    if (licencesByStatus.isNotEmpty()) {
      val licenceToConvert = getLicenceToConvert(licencesByStatus)
      inactivateConversionLicence(OPT_IN, licencesByStatus)

      val hdcCopy = hdcLicenceFactory.createFromCrd(licenceToConvert, IN_PROGRESS)
      val newLicence = licenceService.populateCopy(
        original = licenceToConvert,
        copy = hdcCopy,
      )

      addConversionAudits(licenceToConvert, newLicence, OPT_IN)
      addLicenceConversionEvents(licenceToConvert, newLicence, OPT_IN)
      addLicenceLinking(OPT_IN, licenceToConvert, newLicence)
    }
  }

  @Transactional
  fun postponeLicence(nomsNumber: String) {
    val licencesByStatus = getHdcLicencesByInflightStatusMap(nomsNumber)

    if (licencesByStatus.isEmpty()) {
      log.info("HDC postpone: no eligible HDC licence found for nomsNumber={}", nomsNumber)
      return
    }

    licencesByStatus.values.forEach { recordPostponement(it, isPostponed = true) }

    val approvedLicence = licencesByStatus[APPROVED]
    val editableLicence = licencesByStatus[SUBMITTED] ?: licencesByStatus[IN_PROGRESS]

    approvedLicence?.let { approved ->
      if (editableLicence == null) {
        return revertLicenceToInProgress(approved)
      }

      licenceService.inactivateLicences(
        listOf(approved),
        reason = "HDC licence postponed",
        deactivateInProgressVersions = false,
      )
    }

    editableLicence?.takeIf { it.statusCode == SUBMITTED }?.let(::revertLicenceToInProgress)
  }

  @Transactional
  fun resumeHdcPostponement(nomsNumber: String) {
    val licencesByStatus = getHdcLicencesByInflightStatusMap(nomsNumber)
    if (licencesByStatus.isEmpty()) {
      log.warn("HDC resume: no eligible HDC licence found for nomsNumber={}", nomsNumber)
      return
    }

    licencesByStatus.values.forEach { recordPostponement(it, isPostponed = false) }
    log.info("HDC resume: cleared postponement state for nomsNumber={}", nomsNumber)
  }

  internal fun revertLicenceToInProgress(licence: HdcLicenceEntity) {
    if (licence.statusCode != IN_PROGRESS) {
      val previousStatus = licence.statusCode
      licence.statusCode = IN_PROGRESS
      licence.approvedByUsername = null
      licence.approvedByName = null
      licence.approvedDate = null
      licence.supersededDate = null

      auditService.recordAuditEventHdcLicencePostponed(licence, previousStatus)
    }
  }
}
