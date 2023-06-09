package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.CommunityOffenderManagerRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition

@Service
class LicenceConditionService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val auditEventRepository: AuditEventRepository,
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
  private val licencePolicyService: LicencePolicyService,
) {

  @Transactional
  fun updateStandardConditions(licenceId: Long, request: UpdateStandardConditionDataRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val entityStandardLicenceConditions =
      request.standardLicenceConditions.transformToEntityStandard(licenceEntity, "AP")
    val entityStandardPssConditions = request.standardPssConditions.transformToEntityStandard(licenceEntity, "PSS")

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val updatedLicence = licenceEntity.copy(
      standardConditions = entityStandardLicenceConditions + entityStandardPssConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    /*
    When the function is called, the standard conditions are updated in line with the current policy version but
    the licence will retain the version of the policy it was created with because any additional conditions will be on a previous
    version and not updated. These additional conditions can only be updated via the vary journey.
    */
    val currentPolicyVersion = licencePolicyService.currentPolicy().version

    val changes = mapOf(
      "typeOfChange" to "update",
      "condition" to "standard",
      "changes" to emptyMap<String, Any>()
    )

    licenceRepository.saveAndFlush(updatedLicence)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Standard conditions updated to policy version $currentPolicyVersion for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
  }

  /**
   * Add additional condition. Allows for adding more than one condition of the same type
   * TODO - This function is only called for subsequent calls to upload a map as part of an additional condition only - needs to be refactored
   */
  @Transactional
  fun addAdditionalCondition(
    licenceId: Long,
    conditionType: String,
    request: AddAdditionalConditionRequest,
  ): AdditionalCondition {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val newConditions = licenceEntity.additionalConditions.toMutableList()

    newConditions.add(
      EntityAdditionalCondition(
        conditionVersion = licenceEntity.version!!,
        conditionType = request.conditionType,
        conditionCode = request.conditionCode,
        conditionText = request.conditionText,
        expandedConditionText = request.expandedText,
        conditionCategory = request.conditionCategory,
        licence = licenceEntity,
        conditionSequence = request.sequence,
      ),
    )

    val updatedLicence = licenceEntity.copy(
      additionalConditions = newConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)

    // return the newly added condition.
    val newCondition =
      licenceEntity.additionalConditions.filter { it.conditionCode == request.conditionCode }.maxBy { it.id }

    val changes = mapOf(
      "typeOfChange" to "add",
      "condition" to "additional",
      "changes" to listOf(
        mapOf(
          "conditionCode" to newCondition.conditionCode,
          "conditionType" to newCondition.conditionType
        )
      )
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Added condition for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
    return transform(newCondition)
  }

  @Transactional
  fun deleteAdditionalCondition(licenceId: Long, conditionId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    // return all conditions except condition with submitted conditionId
    val revisedConditions = licenceEntity.additionalConditions.filter { it.id != conditionId }

    val removedCondition =
      licenceEntity.additionalConditions.filter { it.id == conditionId }.maxBy { it.id }

    val updatedLicence = licenceEntity.copy(
      additionalConditions = revisedConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    val changes = mapOf(
      "typeOfChange" to "delete",
      "condition" to "additional",
      "changes" to listOf(
        mapOf(
          "conditionCode" to removedCondition.conditionCode,
          "conditionType" to removedCondition.conditionType,
          "conditionText" to removedCondition.expandedConditionText
        )
      )
    )

    licenceRepository.saveAndFlush(updatedLicence)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Deleted condition for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
  }

  @Transactional
  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")
    val submittedConditions =
      request.additionalConditions.transformToEntityAdditional(licenceEntity, request.conditionType)

    val existingConditions = licenceEntity.additionalConditions

    val newConditions = existingConditions.getAddedConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedConditions(submittedConditions, request)

    val updatedConditions = existingConditions.getUpdatedConditions(submittedConditions, removedConditions)

    val updatedLicence = licenceEntity.copy(
      additionalConditions = newConditions + updatedConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)

    // If any removed additional conditions had a file upload associated then remove the detail row to prevent being orphaned
    removedConditions.forEach { oldCondition ->
      oldCondition.additionalConditionUploadSummary.forEach {
        additionalConditionUploadDetailRepository.findById(it.uploadDetailId).ifPresent { uploadDetail ->
          additionalConditionUploadDetailRepository.delete(uploadDetail)
        }
      }
    }

    /*
    Using this function, an additional condition is either a new condition or to be removed. Updates to existing
    additional conditions only involve updating the data associated with the condition using the updateAdditionalConditionData
    function
     */
    val changes: Map<String, Any>
    if (newAdditionalConditions.isNotEmpty()) {
      changes = mapOf(
        "typeOfChange" to "add",
        "condition" to "additional",
        "changes" to
          newAdditionalConditions.map {
            mapOf(
              "conditionCode" to it.conditionCode,
              "conditionType" to it.conditionType,
            )
          }
      )
    } else {
      changes = mapOf(
        "typeOfChange" to "remove",
        "condition" to "additional",
        "changes" to
          removedAdditionalConditionsList.map {
            mapOf(
              "conditionCode" to it.conditionCode,
              "conditionType" to it.conditionType
            )
          }
      )
    }

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Updated multiple conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
  }

  private fun List<EntityAdditionalCondition>.getUpdatedConditions(
    submittedAdditionalConditions: List<EntityAdditionalCondition>,
    removedAdditionalConditionsList: List<EntityAdditionalCondition>,
  ): List<EntityAdditionalCondition> {
    val updatedAdditionalConditions =
      this.filter { condition -> removedAdditionalConditionsList.none { it.conditionCode == condition.conditionCode } }

    submittedAdditionalConditions.forEach { newAdditionalCondition ->
      updatedAdditionalConditions.filter {
        it.conditionCode == newAdditionalCondition.conditionCode
      }.forEach {
        it.conditionCategory = newAdditionalCondition.conditionCategory
        it.conditionText = newAdditionalCondition.conditionText
        it.conditionSequence = newAdditionalCondition.conditionSequence
        it.conditionType = newAdditionalCondition.conditionType
      }
    }
    /*
    Using this function, an additional condition is either a new condition or to be removed. Updates to existing
    additional conditions only involve updating the data associated with the condition using the updateAdditionalConditionData
    function
     */
    val changes: Map<String, Any>
    if (newAdditionalConditions.isNotEmpty()) {
      changes = mapOf(
        "typeOfChange" to "add",
        "condition" to "additional",
        "changes" to
          newAdditionalConditions.map {
            mapOf(
              "conditionCode" to it.conditionCode,
              "conditionType" to it.conditionType,
            )
          }
      )
    } else {
      changes = mapOf(
        "typeOfChange" to "remove",
        "condition" to "additional",
        "changes" to
          removedAdditionalConditionsList.map {
            mapOf(
              "conditionCode" to it.conditionCode,
              "conditionType" to it.conditionType
            )
          }
      )
    }

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Updated multiple conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
    return updatedAdditionalConditions
  }

  private fun List<EntityAdditionalCondition>.getRemovedConditions(
    submittedAdditionalConditions: List<EntityAdditionalCondition>,
    request: AdditionalConditionsRequest,
  ) = this.filter {
    it.conditionType == request.conditionType && submittedAdditionalConditions.none { submittedCondition -> submittedCondition.conditionCode == it.conditionCode }
  }

  private fun List<EntityAdditionalCondition>.getAddedConditions(
    submittedConditions: List<EntityAdditionalCondition>,
  ) =
    submittedConditions
      .filter { this.none { submittedCondition -> submittedCondition.conditionCode == it.conditionCode } }
      .map { newAdditionalCondition ->
        newAdditionalCondition.copy(
          expandedConditionText = newAdditionalCondition.conditionText,
        )
      }

  @Transactional
  fun updateBespokeConditions(licenceId: Long, request: BespokeConditionRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val updatedLicence = licenceEntity.copy(
      bespokeConditions = emptyList(),
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)

    // Replace the bespoke conditions
    request.conditions.forEachIndexed { index, condition ->
      bespokeConditionRepository.saveAndFlush(
        BespokeCondition(licence = licenceEntity, conditionSequence = index, conditionText = condition),
      )
    }

    val changes = mapOf(
      "typeOfChange" to "update",
      "condition" to "bespoke",
      "changes" to request.conditions
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Updated bespoke conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
  }

  @Transactional
  fun updateAdditionalConditionData(
    licenceId: Long,
    additionalConditionId: Long,
    request: UpdateAdditionalConditionDataRequest,
  ) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val additionalCondition = additionalConditionRepository
      .findById(additionalConditionId)
      .orElseThrow { EntityNotFoundException("$additionalConditionId") }

    val updatedAdditionalCondition = additionalCondition.copy(
      conditionVersion = licenceEntity.version!!,
      additionalConditionData = request.data.transformToEntityAdditionalData(additionalCondition),
      expandedConditionText = request.expandedConditionText,
    )
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)

    val username = SecurityContextHolder.getContext().authentication.name
    val createdBy = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val updatedLicence = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicence)

    val changes = mapOf(
      "typeOfChange" to "update",
      "condition" to "additional data",
      "changes" to listOf(
        mapOf(
          "conditionCode" to updatedAdditionalCondition.conditionCode,
          "conditionType" to updatedAdditionalCondition.conditionType,
          "conditionText" to updatedAdditionalCondition.expandedConditionText
        )
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = username,
        fullName = "${createdBy.firstName} ${createdBy.lastName}",
        summary = "Updated additional condition data for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes
      )
    )
  }
}
