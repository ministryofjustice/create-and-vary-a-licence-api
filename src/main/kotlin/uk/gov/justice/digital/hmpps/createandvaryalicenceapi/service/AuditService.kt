package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AuditEvent as ModelAuditEvent

@Service
class AuditService(
  private val auditEventRepository: AuditEventRepository,
  private val licenceRepository: LicenceRepository,
) {

  fun recordAuditEvent(auditEvent: ModelAuditEvent) {
    auditEventRepository.save(transform(auditEvent))
  }

  fun getAuditEvents(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return if (auditRequest.licenceId != null && auditRequest.username != null) {
      getAuditEventsForLicenceAndUser(auditRequest)
    } else if (auditRequest.licenceId != null) {
      getAuditEventsForLicence(auditRequest)
    } else if (auditRequest.username != null) {
      getAuditEventsForUser(auditRequest)
    } else {
      getAllEvents(auditRequest)
    }
  }

  fun recordAuditEventUpdateStandardCondition(licence: Licence, currentUser: CommunityOffenderManager, currentPolicyVersion: String) {
    val summary = "Updated standard conditions to policy version $currentPolicyVersion"

    val changes = mapOf(
      "type" to "Updated standard conditions",
      "changes" to emptyMap<String, Any>(),
    )

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventAddAdditionalConditionOfSameType(licence: Licence, currentUser: CommunityOffenderManager, condition: AdditionalCondition) {
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventDeleteAdditionalConditions(
    licence: Licence,
    currentUser: CommunityOffenderManager,
    removedAdditionalConditions: List<AdditionalCondition>,
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventDeleteStandardConditions(
    licence: Licence,
    currentUser: CommunityOffenderManager,
    removedStandardConditions: List<StandardCondition>,
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventDeleteBespokeConditions(
    licence: Licence,
    currentUser: CommunityOffenderManager,
    removedBespokeConditions: List<BespokeCondition>,
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventUpdateAdditionalConditions(
    licence: Licence,
    currentUser: CommunityOffenderManager,
    newConditions: List<AdditionalCondition>,
    removedConditions: List<AdditionalCondition>,
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventUpdateBespokeConditions(
    licence: Licence,
    currentUser: CommunityOffenderManager,
    newConditions: List<String>,
    removedConditions: List<String?>,
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
  }

  fun recordAuditEventUpdateAdditionalConditionData(licence: Licence, currentUser: CommunityOffenderManager, condition: AdditionalCondition) {
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

    auditEventRepository.save(createAuditEvent(licence, currentUser, summary, changes))
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

  private fun getAuditEventsForUser(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return auditEventRepository
      .findAllByUsernameAndEventTimeBetweenOrderByEventTimeDesc(
        auditRequest.username!!,
        auditRequest.startTime,
        auditRequest.endTime,
      )
      .transformToModelAuditEvents()
  }

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

  private fun getAllEvents(auditRequest: AuditRequest): List<ModelAuditEvent> {
    return auditEventRepository
      .findAllByEventTimeBetweenOrderByEventTimeDesc(auditRequest.startTime, auditRequest.endTime)
      .transformToModelAuditEvents()
  }

  private fun createAuditEvent(licence: Licence, currentUser: CommunityOffenderManager, summary: String, changes: Map<String, Any>) =
    AuditEvent(
      licenceId = licence.id,
      username = currentUser.username,
      fullName = "${currentUser.firstName} ${currentUser.lastName}",
      summary = "$summary for ${licence.forename} ${licence.surname}",
      detail = "ID ${licence.id} type ${licence.typeCode} status ${licence.statusCode.name} version ${licence.version}",
      changes = changes,
    )
}
