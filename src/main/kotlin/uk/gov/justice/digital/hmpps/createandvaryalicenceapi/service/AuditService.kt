package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent

@Service
class AuditService(
  private val auditEventRepository: AuditEventRepository,
  private val licenceRepository: LicenceRepository,
) {

  fun recordAuditEvent(auditEvent: ModelAuditEvent) {
    auditEventRepository.save(transform(auditEvent))
  }

  fun getAuditEvents(auditRequest: AuditRequest): List<ModelAuditEvent> = if (auditRequest.licenceId != null && auditRequest.username != null) {
    getAuditEventsForLicenceAndUser(auditRequest)
  } else if (auditRequest.licenceId != null) {
    getAuditEventsForLicence(auditRequest)
  } else if (auditRequest.username != null) {
    getAuditEventsForUser(auditRequest)
  } else {
    getAllEvents(auditRequest)
  }

  fun recordAuditEventUpdateStandardCondition(
    licence: Licence,
    currentPolicyVersion: String,
    staffMember: Staff?,
  ) {
    val summary = "Updated standard conditions to policy version $currentPolicyVersion"

    val changes = mapOf(
      "type" to "Updated standard conditions",
      "changes" to emptyMap<String, Any>(),
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventAddAdditionalConditionOfSameType(
    licence: Licence,
    condition: AdditionalCondition,
    staffMember: Staff?,
  ) {
    val summary = "Updated additional condition of the same type"

    val changes = mapOf(
      "type" to "Updated additional conditions",
      "changes" to listOf(
        mapOf(
          "type" to "ADDED",
          "conditionCode" to condition.conditionCode,
          "conditionType" to condition.conditionType,
          "conditionText" to condition.conditionText,
        ),
      ),
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventDeleteAdditionalConditions(
    licence: Licence,
    removedAdditionalConditions: List<AdditionalCondition>,
    staffMember: Staff?,
  ) {
    if (removedAdditionalConditions.isEmpty()) {
      return
    }
    val summary = "Updated additional conditions"

    val changes = mapOf(
      "type" to summary,
      "changes" to removedAdditionalConditions.map {
        mapOf(
          "type" to "REMOVED",
          "conditionCode" to it.conditionCode,
          "conditionType" to it.conditionType,
          "conditionText" to it.expandedConditionText,
        )
      },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventDeleteStandardConditions(
    licence: Licence,
    removedStandardConditions: List<StandardCondition>,
    staffMember: Staff?,
  ) {
    if (removedStandardConditions.isEmpty()) {
      return
    }
    val summary = "Updated standard conditions"

    val changes = mapOf(
      "type" to summary,
      "changes" to removedStandardConditions.map {
        mapOf(
          "type" to "REMOVED",
          "conditionCode" to it.conditionCode,
          "conditionType" to it.conditionType,
          "conditionText" to it.conditionText,
        )
      },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventDeleteBespokeConditions(
    licence: Licence,
    removedBespokeConditions: List<BespokeCondition>,
    staffMember: Staff?,
  ) {
    if (removedBespokeConditions.isEmpty()) {
      return
    }
    val summary = "Updated bespoke conditions"

    val changes = mapOf(
      "type" to summary,
      "changes" to removedBespokeConditions.map {
        mapOf(
          "type" to "REMOVED",
          "conditionText" to it.conditionText,
        )
      },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventUpdateAdditionalConditions(
    licence: Licence,
    newConditions: List<AdditionalCondition>,
    removedConditions: List<AdditionalCondition>,
    staffMember: Staff?,
  ) {
    if (newConditions.isEmpty() && removedConditions.isEmpty()) {
      return
    }
    val summary = "Updated additional conditions"

    val changes = mapOf(
      "type" to summary,
      "changes" to newConditions.map {
        mapOf(
          "type" to "ADDED",
          "conditionCode" to it.conditionCode,
          "conditionType" to it.conditionType,
          "conditionText" to it.conditionText,
        )
      } +
        removedConditions.map {
          mapOf(
            "type" to "REMOVED",
            "conditionCode" to it.conditionCode,
            "conditionType" to it.conditionType,
            "conditionText" to it.conditionText,
          )
        },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventUpdateBespokeConditions(
    licence: Licence,
    newConditions: List<String>,
    removedConditions: List<String?>,
    staffMember: Staff?,
  ) {
    if (newConditions.isEmpty() && removedConditions.isEmpty()) {
      return
    }
    val summary = "Updated bespoke conditions"

    val changes = mapOf(
      "type" to summary,
      "changes" to newConditions.map {
        mapOf(
          "type" to "ADDED",
          "conditionText" to it,
        )
      } +
        removedConditions.map {
          mapOf(
            "type" to "REMOVED",
            "conditionText" to it,
          )
        },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventUpdateAdditionalConditionData(
    licence: Licence,
    condition: AdditionalCondition,
    staffMember: Staff?,
  ) {
    val summary = "Updated additional condition data"

    val changes = mapOf(
      "type" to summary,
      "changes" to listOf(
        mapOf(
          "type" to "ADDED",
          "conditionCode" to condition.conditionCode,
          "conditionType" to condition.conditionType,
          "conditionText" to condition.expandedConditionText,
        ),
      ),
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventUpdateElectronicMonitoringProgramme(
    licence: Licence,
    electronicMonitoringProgrammeRequest: UpdateElectronicMonitoringProgrammeRequest,
    staffMember: Staff?,
  ) {
    val summary = "Updated electronic monitoring programme details"

    val changes = mapOf(
      "type" to summary,
      "changes" to mapOf(
        "isToBeTaggedForProgramme" to electronicMonitoringProgrammeRequest.isToBeTaggedForProgramme,
        "programmeName" to electronicMonitoringProgrammeRequest.programmeName,
      ),
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventInitialAppointmentUpdate(licence: Licence, changes: Map<String, Any>, staffMember: Staff?) {
    val summary = "Updated initial appointment details"

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  fun recordAuditEventUpdateHdcCurfewTimes(
    licence: Licence,
    updatedCurfewTimes: List<HdcCurfewTimes>,
    staffMember: Staff?,
  ) {
    val summary = "Updated HDC curfew times"

    val changes = mapOf(
      "type" to summary,
      "changes" to updatedCurfewTimes.map {
        mapOf(
          "fromDay" to it.fromDay,
          "fromTime" to it.fromTime,
          "untilDay" to it.untilDay,
          "untilTime" to it.untilTime,
        )
      },
    )

    auditEventRepository.save(createAuditEvent(licence, summary, changes, staffMember))
  }

  private fun getAuditEventsForLicence(auditRequest: AuditRequest): List<ModelAuditEvent> {
    licenceRepository
      .findById(auditRequest.licenceId!!)
      .orElseThrow { EntityNotFoundException("${auditRequest.licenceId}") }

    return auditEventRepository
      .findAllByLicenceIdAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.licenceId,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

  private fun getAuditEventsForUser(auditRequest: AuditRequest): List<ModelAuditEvent> = auditEventRepository
    .findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
      auditRequest.username!!,
      auditRequest.startTime,
      auditRequest.endTime,
    )
    .transformToModelAuditEvents()

  private fun getAuditEventsForLicenceAndUser(auditRequest: AuditRequest): List<ModelAuditEvent> {
    licenceRepository
      .findById(auditRequest.licenceId!!)
      .orElseThrow { EntityNotFoundException("${auditRequest.licenceId}") }

    return auditEventRepository
      .findAllByLicenceIdAndUsernameAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.licenceId,
        auditRequest.username!!,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

  private fun getAllEvents(auditRequest: AuditRequest): List<ModelAuditEvent> = auditEventRepository
    .findAllByEventTimeBetweenOrderByEventTimeDesc(auditRequest.startTime, auditRequest.endTime)
    .transformToModelAuditEvents()

  private fun createAuditEvent(
    licence: Licence,
    summary: String,
    changes: Map<String, Any>,
    staffMember: Staff?,
  ): AuditEvent = AuditEvent(
    licenceId = licence.id,
    username = staffMember?.username ?: "SYSTEM",
    fullName = if (staffMember != null) "${staffMember.firstName} ${staffMember.lastName}" else "SYSTEM",
    eventType = if (staffMember == null) AuditEventType.SYSTEM_EVENT else AuditEventType.USER_EVENT,
    summary = "$summary for ${licence.forename} ${licence.surname}",
    detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
    changes = changes,
  )
}
