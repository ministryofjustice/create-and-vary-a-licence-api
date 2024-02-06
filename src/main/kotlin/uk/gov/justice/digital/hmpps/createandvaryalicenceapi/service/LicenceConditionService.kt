package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition

@Service
class LicenceConditionService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val conditionFormatter: ConditionFormatter,
  private val licencePolicyService: LicencePolicyService,
  private val auditService: AuditService,
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

    val updatedLicence = licenceEntity.updateConditions(
      updatedStandardConditions = entityStandardLicenceConditions + entityStandardPssConditions,
      updatedByUsername = username,
    )

    val currentPolicyVersion = licencePolicyService.currentPolicy().version

    licenceRepository.saveAndFlush(updatedLicence)
    auditService.recordAuditEventUpdateStandardCondition(licenceEntity, currentPolicyVersion)
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

    val updatedLicence = licenceEntity.updateConditions(
      updatedAdditionalConditions = newConditions,
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)

    // return the newly added condition.
    val newCondition =
      licenceEntity.additionalConditions.filter { it.conditionCode == request.conditionCode }.maxBy { it.id }

    auditService.recordAuditEventAddAdditionalConditionOfSameType(licenceEntity, newCondition)

    val readyToSubmit = isConditionReadyToSubmit(
      newCondition,
      licencePolicyService.getAllAdditionalConditions(),
    )

    return transform(newCondition, readyToSubmit)
  }

  @Transactional
  fun deleteAdditionalCondition(licenceId: Long, conditionId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    deleteConditions(licenceEntity, listOf(conditionId), emptyList(), emptyList())
  }

  @Transactional
  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val submittedConditions =
      request.additionalConditions.transformToEntityAdditional(licenceEntity, request.conditionType)

    val existingConditions = licenceEntity.additionalConditions

    val newConditions = existingConditions.getAddedConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedConditions(submittedConditions, request)

    val updatedConditions = existingConditions.getUpdatedConditions(submittedConditions, removedConditions)

    val updatedLicence = licenceEntity.updateConditions(
      updatedAdditionalConditions = (newConditions + updatedConditions),
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

    auditService.recordAuditEventUpdateAdditionalConditions(
      licenceEntity,
      newConditions,
      removedConditions,
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

    val submittedConditions = request.conditions

    val existingConditions = licenceEntity.bespokeConditions

    val newConditions = existingConditions.getAddedBespokeConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedBespokeConditions(submittedConditions)

    // if the same bespoke conditions are submitted, return early
    if (newConditions.isEmpty() && removedConditions.isEmpty()) {
      return
    }

    val updatedLicence = licenceEntity.updateConditions(
      updatedBespokeConditions = emptyList(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)

    // Replace the bespoke conditions
    request.conditions.forEachIndexed { index, condition ->
      bespokeConditionRepository.saveAndFlush(
        BespokeCondition(licence = licenceEntity, conditionSequence = index, conditionText = condition),
      )
    }

    auditService.recordAuditEventUpdateBespokeConditions(licenceEntity, newConditions, removedConditions)
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

    val version = licenceEntity.version!!
    val conditionCode = additionalCondition.conditionCode!!
    val additionalConditionData = request.data.transformToEntityAdditionalData(additionalCondition)

    val updatedAdditionalCondition = additionalCondition.copy(
      conditionVersion = version,
      additionalConditionData = additionalConditionData,
      expandedConditionText = getFormattedText(version, conditionCode, additionalConditionData),
    )
    additionalConditionRepository.saveAndFlush(updatedAdditionalCondition)

    val username = SecurityContextHolder.getContext().authentication.name

    val updatedLicence = licenceEntity.updateConditions(updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicence)

    auditService.recordAuditEventUpdateAdditionalConditionData(licenceEntity, updatedAdditionalCondition)
  }

  fun getFormattedText(version: String, conditionCode: String, data: List<AdditionalConditionData>) =
    conditionFormatter.format(licencePolicyService.getConfigForCondition(version, conditionCode), data)

  @Transactional
  fun deleteConditions(
    licenceEntity: Licence,
    additionalConditionIds: List<Long>,
    standardConditionIds: List<Long>,
    bespokeConditionIds: List<Long>,
  ) {
    if (standardConditionIds.isEmpty() && additionalConditionIds.isEmpty() && bespokeConditionIds.isEmpty()) {
      return
    }
    val username = SecurityContextHolder.getContext().authentication.name

    // return all conditions except condition with submitted conditionIds
    val revisedAdditionalConditions =
      licenceEntity.additionalConditions.filter { !additionalConditionIds.contains(it.id) }

    val removedAdditionalConditions =
      licenceEntity.additionalConditions.filter { additionalConditionIds.contains(it.id) }

    // return all conditions except condition with submitted conditionIds
    val revisedStandardConditions =
      licenceEntity.standardConditions.filter { !standardConditionIds.contains(it.id) }

    val removedStandardConditions =
      licenceEntity.standardConditions.filter { standardConditionIds.contains(it.id) }

    // return all conditions except condition with submitted conditionIds
    val revisedBespokeConditions =
      licenceEntity.bespokeConditions.filter { !bespokeConditionIds.contains(it.id) }

    val removedBespokeConditions =
      licenceEntity.bespokeConditions.filter { bespokeConditionIds.contains(it.id) }

    val updatedLicence = licenceEntity.updateConditions(
      updatedAdditionalConditions = revisedAdditionalConditions,
      updatedStandardConditions = revisedStandardConditions,
      updatedBespokeConditions = revisedBespokeConditions,
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)

    auditService.recordAuditEventDeleteAdditionalConditions(licenceEntity, removedAdditionalConditions)
    auditService.recordAuditEventDeleteStandardConditions(licenceEntity, removedStandardConditions)
    auditService.recordAuditEventDeleteBespokeConditions(licenceEntity, removedBespokeConditions)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
