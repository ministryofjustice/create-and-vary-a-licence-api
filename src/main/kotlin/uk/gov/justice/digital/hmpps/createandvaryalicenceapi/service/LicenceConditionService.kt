package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
  private val conditionFormatter: ConditionFormatter,
  private val licencePolicyService: LicencePolicyService,
  private val auditEventRepository: AuditEventRepository,
  private val communityOffenderManagerRepository: CommunityOffenderManagerRepository,
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
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val updatedLicence = licenceEntity.copy(
      standardConditions = entityStandardLicenceConditions + entityStandardPssConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    val currentPolicyVersion = licencePolicyService.currentPolicy().version

    val changes = mapOf(
      "type" to "Update standard conditions",
      "changes" to emptyMap<String, Any>(),
    )

    licenceRepository.saveAndFlush(updatedLicence)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated standard conditions to policy version $currentPolicyVersion for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
    )
  }

  /**
   * Add additional condition. Allows for adding more than one condition of the same type
   */
  @Transactional
  fun addAdditionalCondition(
    licenceId: Long,
    request: AddAdditionalConditionRequest,
  ): AdditionalCondition {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
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
      "type" to "Update additional conditions",
      "changes" to listOf(
        mapOf(
          "type" to "ADDED",
          "conditionCode" to newCondition.conditionCode,
          "conditionType" to newCondition.conditionType,
          "conditionText" to newCondition.conditionText,
        ),
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated additional condition of the same type for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
    )
    return transform(newCondition)
  }

  @Transactional
  fun deleteAdditionalCondition(licenceId: Long, conditionId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
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
      "type" to "Update additional conditions",
      "changes" to listOf(
        mapOf(
          "type" to "REMOVED",
          "conditionCode" to removedCondition.conditionCode,
          "conditionType" to removedCondition.conditionType,
          "conditionText" to removedCondition.expandedConditionText,
        ),
      ),
    )

    licenceRepository.saveAndFlush(updatedLicence)

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated additional conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
    )
  }

  @Transactional
  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val submittedConditions =
      request.additionalConditions.transformToEntityAdditional(licenceEntity, request.conditionType)

    val existingConditions = licenceEntity.additionalConditions

    val newConditions = existingConditions.getAddedConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedConditions(submittedConditions, request)

    val updatedConditions = existingConditions.getUpdatedConditions(submittedConditions, removedConditions)

    val updatedLicence = licenceEntity.copy(
      additionalConditions = (newConditions + updatedConditions).onEach { checkFormattedText(it) },
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

    val changes = mapOf(
      "type" to "Update additional conditions",
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

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated additional conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
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
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val submittedConditions = request.conditions

    val existingConditions = licenceEntity.bespokeConditions

    val newConditions = existingConditions.getAddedBespokeConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedBespokeConditions(submittedConditions)

    // if the same bespoke conditions are submitted, return early
    if (newConditions.isEmpty() && removedConditions.isEmpty()) {
      return
    }

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
      "type" to "Update bespoke conditions",
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

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated bespoke conditions for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
    )
  }

  private fun List<BespokeCondition>.getAddedBespokeConditions(
    submittedConditions: List<String>,
  ) =
    submittedConditions
      .filter { this.none { existingCondition -> existingCondition.conditionText == it } }

  private fun List<BespokeCondition>.getRemovedBespokeConditions(
    submittedConditions: List<String>,
  ) = this.filter {
    submittedConditions.none { conditionText -> conditionText == it.conditionText }
  }.map {
    it.conditionText
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
    checkFormattedText(updatedAdditionalCondition)
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)

    val username = SecurityContextHolder.getContext().authentication.name
    val currentUser = communityOffenderManagerRepository.findByUsernameIgnoreCase(username)
      ?: throw RuntimeException("Staff with username $username not found")

    val updatedLicence = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicence)

    val changes = mapOf(
      "type" to "Update additional condition data",
      "changes" to listOf(
        mapOf(
          "type" to "ADDED",
          "conditionCode" to updatedAdditionalCondition.conditionCode,
          "conditionType" to updatedAdditionalCondition.conditionType,
          "conditionText" to updatedAdditionalCondition.expandedConditionText,
        ),
      ),
    )

    auditEventRepository.saveAndFlush(
      AuditEvent(
        licenceId = licenceId,
        username = currentUser.username,
        fullName = "${currentUser.firstName} ${currentUser.lastName}",
        summary = "Updated additional condition data for ${licenceEntity.forename} ${licenceEntity.surname}",
        detail = "ID ${licenceEntity.id} type ${licenceEntity.typeCode} status ${licenceEntity.statusCode.name} version ${licenceEntity.version}",
        changes = changes,
      ),
    )
  }

  fun checkFormattedText(additionalCondition: EntityAdditionalCondition) {
    try {
      val conditionConfig = licencePolicyService.getConfigForCondition(additionalCondition)
      val backendText = conditionFormatter.format(conditionConfig, additionalCondition.additionalConditionData)
      val frontendText = additionalCondition.expandedConditionText
      if (backendText != frontendText) {
        log.warn("FormattingInconsistency: condition of type: ${conditionConfig.code}, licence: ${additionalCondition.licence.id}")
      } else {
        log.info("FormattingMatch: condition of type: ${conditionConfig.code}, licence: ${additionalCondition.licence.id}")
      }
    } catch (e: RuntimeException) {
      log.error(
        "FormattingError: condition of type: ${additionalCondition.conditionCode}, licence: ${additionalCondition.licence.id}",
        e,
      )
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
