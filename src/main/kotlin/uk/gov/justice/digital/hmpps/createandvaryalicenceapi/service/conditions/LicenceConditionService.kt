package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalConditionData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AdditionalConditionsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.BespokeConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateAdditionalConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateStandardConditionDataRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAdditionalConditionRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.DeleteAdditionalConditionsByCodeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.ElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AdditionalConditionUploadDetailRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.BespokeConditionRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transform
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToEntityAdditional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToEntityAdditionalData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToEntityStandard
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AdditionalCondition as EntityAdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider as EntityElectronicMonitoringProvider

@Service
class LicenceConditionService(
  private val licenceRepository: LicenceRepository,
  private val additionalConditionRepository: AdditionalConditionRepository,
  private val bespokeConditionRepository: BespokeConditionRepository,
  private val additionalConditionUploadDetailRepository: AdditionalConditionUploadDetailRepository,
  private val conditionFormatter: ConditionFormatter,
  private val licencePolicyService: LicencePolicyService,
  private val auditService: AuditService,
  private val staffRepository: StaffRepository,
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

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateConditions(
      updatedStandardConditions = entityStandardLicenceConditions + entityStandardPssConditions,
      staffMember = staffMember,
    )

    val currentPolicyVersion = licencePolicyService.currentPolicy().version

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventUpdateStandardCondition(licenceEntity, currentPolicyVersion, staffMember)
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

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

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

    licenceEntity.updateConditions(
      updatedAdditionalConditions = newConditions,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)

    // return the newly added condition.
    val newCondition =
      licenceEntity.additionalConditions.filter { it.conditionCode == request.conditionCode }.maxBy { it.id }

    auditService.recordAuditEventAddAdditionalConditionOfSameType(licenceEntity, newCondition, staffMember)

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
  fun deleteAdditionalConditionsByCode(licenceId: Long, request: DeleteAdditionalConditionsByCodeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }
    val conditionIds = licenceEntity.additionalConditions.filter {
      request.conditionCodes.contains(it.conditionCode)
    }.map { it.id }
    deleteConditions(licenceEntity, conditionIds, emptyList(), emptyList())
  }

  @Transactional
  fun updateAdditionalConditions(licenceId: Long, request: AdditionalConditionsRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val submittedConditions =
      request.additionalConditions.transformToEntityAdditional(licenceEntity, request.conditionType)

    val existingConditions = licenceEntity.additionalConditions

    val newConditions = existingConditions.getAddedConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedConditions(submittedConditions, request)

    val updatedConditions = existingConditions.getUpdatedConditions(submittedConditions, removedConditions)

    licenceEntity.updateConditions(
      updatedAdditionalConditions = (newConditions + updatedConditions),
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)

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
      staffMember,
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
  ) = submittedConditions
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

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val submittedConditions = request.conditions

    val existingConditions = licenceEntity.bespokeConditions

    val newConditions = existingConditions.getAddedBespokeConditions(submittedConditions)

    val removedConditions = existingConditions.getRemovedBespokeConditions(submittedConditions)

    // if the same bespoke conditions are submitted, return early
    if (newConditions.isEmpty() && removedConditions.isEmpty()) {
      return
    }

    licenceEntity.updateConditions(
      updatedBespokeConditions = emptyList(),
      staffMember = staffMember,
    )
    licenceRepository.saveAndFlush(licenceEntity)

    // Replace the bespoke conditions
    request.conditions.forEachIndexed { index, condition ->
      bespokeConditionRepository.saveAndFlush(
        BespokeCondition(licence = licenceEntity, conditionSequence = index, conditionText = condition),
      )
    }

    auditService.recordAuditEventUpdateBespokeConditions(licenceEntity, newConditions, removedConditions, staffMember)
  }

  private fun List<BespokeCondition>.getAddedBespokeConditions(
    submittedConditions: List<String>,
  ) = submittedConditions
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

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateConditions(staffMember = staffMember)
    licenceRepository.saveAndFlush(licenceEntity)

    auditService.recordAuditEventUpdateAdditionalConditionData(licenceEntity, updatedAdditionalCondition, staffMember)
  }

  fun getFormattedText(version: String, conditionCode: String, data: List<AdditionalConditionData>) = conditionFormatter.format(licencePolicyService.getConfigForCondition(version, conditionCode), data)

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

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

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

    licenceEntity.updateConditions(
      updatedAdditionalConditions = revisedAdditionalConditions,
      updatedStandardConditions = revisedStandardConditions,
      updatedBespokeConditions = revisedBespokeConditions,
      staffMember = staffMember,
    )
    licenceRepository.saveAndFlush(licenceEntity)

    auditService.recordAuditEventDeleteAdditionalConditions(licenceEntity, removedAdditionalConditions, staffMember)
    auditService.recordAuditEventDeleteStandardConditions(licenceEntity, removedStandardConditions, staffMember)
    auditService.recordAuditEventDeleteBespokeConditions(licenceEntity, removedBespokeConditions, staffMember)
  }

  @Transactional
  fun updateElectronicMonitoringProgramme(licenceId: Long, request: ElectronicMonitoringProgrammeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val electronicMonitoringProvider = EntityElectronicMonitoringProvider(
      isToBeTaggedForProgramme = request.isToBeTaggedForProgramme,
      programmeName = request.programmeName,
      licence = licenceEntity,
    )

    when (licenceEntity) {
      is CrdLicence -> licenceEntity.electronicMonitoringProvider = electronicMonitoringProvider
      is HdcLicence -> licenceEntity.electronicMonitoringProvider = electronicMonitoringProvider
      else -> error("Trying to update electronic monitoring provider details for non-crd or non-hdc: $licenceId")
    }
    licenceRepository.saveAndFlush(licenceEntity)

    auditService.recordAuditEventUpdateElectronicMonitoringProgramme(licenceEntity, request, staffMember)
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
