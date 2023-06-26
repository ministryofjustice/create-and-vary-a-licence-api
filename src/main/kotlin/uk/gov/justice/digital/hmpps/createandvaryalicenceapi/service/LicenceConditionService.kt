package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.BespokeCondition
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

    val updatedLicence = licenceEntity.copy(
      standardConditions = entityStandardLicenceConditions + entityStandardPssConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )
    licenceRepository.saveAndFlush(updatedLicence)
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

    val updatedLicence = licenceEntity.copy(
      additionalConditions = newConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)

    // return the newly added condition.
    val newCondition =
      licenceEntity.additionalConditions.filter { it.conditionCode == request.conditionCode }.maxBy { it.id }

    return transform(newCondition)
  }

  @Transactional
  fun deleteAdditionalCondition(licenceId: Long, conditionId: Long) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name

    // return all conditions except condition with submitted conditionId
    val revisedConditions = licenceEntity.additionalConditions.filter { it.id != conditionId }

    val updatedLicence = licenceEntity.copy(
      additionalConditions = revisedConditions,
      dateLastUpdated = LocalDateTime.now(),
      updatedByUsername = username,
    )

    licenceRepository.saveAndFlush(updatedLicence)
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
    val updatedLicence = licenceEntity.copy(dateLastUpdated = LocalDateTime.now(), updatedByUsername = username)
    licenceRepository.saveAndFlush(updatedLicence)
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
