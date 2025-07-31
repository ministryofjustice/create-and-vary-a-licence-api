package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HasElectronicMonitoringResponseProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService

@Service
class ElectronicMonitoringProgrammeService(
  private val licenceRepository: LicenceRepository,
  private val licencePolicyService: LicencePolicyService,
  private val auditService: AuditService,
  private val staffRepository: StaffRepository,
  @param:Value("\${feature.toggle.electronicMonitoringResponseHandling:false}")
  private val electronicMonitoringResponseHandlingEnabled: Boolean = false,
) {

  @Transactional
  fun handleUpdatedConditionsIfEnabled(licence: Licence, conditionCodes: Set<String>) {
    if (electronicMonitoringResponseHandlingEnabled) {
      processUpdatedElectronicMonitoringConditions(licence, conditionCodes)
    }
  }

  @Transactional
  fun handleRemovedConditionsIfEnabled(licence: Licence, removedConditionCodes: Set<String>) {
    if (electronicMonitoringResponseHandlingEnabled) {
      processRemovedElectronicMonitoringConditions(licence, removedConditionCodes)
    }
  }

  @Transactional
  fun updateElectronicMonitoringProgramme(licenceId: Long, request: UpdateElectronicMonitoringProgrammeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val electronicMonitoringProvider = when (licenceEntity) {
      is PrrdLicence -> requireNotNull(licenceEntity.electronicMonitoringProvider) {
        "ElectronicMonitoringProvider is null for PrrdLicence: $licenceId"
      }

      is CrdLicence -> requireNotNull(licenceEntity.electronicMonitoringProvider) {
        "ElectronicMonitoringProvider is null for CrdLicence: $licenceId"
      }

      is HdcLicence -> requireNotNull(licenceEntity.electronicMonitoringProvider) {
        "ElectronicMonitoringProvider is null for HdcLicence: $licenceId"
      }

      else -> error("Trying to update electronic monitoring provider details for non-crd or non-hdc: $licenceId")
    }
    electronicMonitoringProvider.isToBeTaggedForProgramme = request.isToBeTaggedForProgramme
    electronicMonitoringProvider.programmeName = request.programmeName
    licenceRepository.saveAndFlush(licenceEntity)

    auditService.recordAuditEventUpdateElectronicMonitoringProgramme(licenceEntity, request, staffMember)
  }

  @Transactional
  fun processUpdatedElectronicMonitoringConditions(licenceEntity: Licence, conditionCodes: Set<String>) {
    val version = requireNotNull(licenceEntity.version) { "Licence version cannot be null" }
    val isResponseRequired = licencePolicyService.isElectronicMonitoringResponseRequired(conditionCodes, version)
    if (isResponseRequired) {
      createElectronicMonitoringProviderIfNotExists(licenceEntity)
    }
  }

  @Transactional
  fun processRemovedElectronicMonitoringConditions(licenceEntity: Licence, removedConditionCodes: Set<String>) {
    val version = requireNotNull(licenceEntity.version) { "Licence version cannot be null" }
    val removedConditionsRequiringEmResponse =
      licencePolicyService.getConditionsRequiringElectronicMonitoringResponse(version, removedConditionCodes)
    val currentConditionCodes = licenceEntity.additionalConditions.map { it.conditionCode }.toSet()
    if (removedConditionsRequiringEmResponse.isNotEmpty() &&
      !licencePolicyService.isElectronicMonitoringResponseRequired(
        currentConditionCodes,
        version,
      )
    ) {
      deleteElectronicMonitoringProvider(licenceEntity)
    }
  }

  @Transactional
  fun deleteElectronicMonitoringProvider(licenceEntity: Licence) {
    log.info("Clearing Electronic Monitoring response records for licence: ${licenceEntity.id}")
    if (licenceEntity is HasElectronicMonitoringResponseProvider) {
      licenceEntity.electronicMonitoringProvider = null
    } else {
      error("ElectronicMonitoringProvider can only be deleted for licences that implement HasElectronicMonitorResponseProvider")
    }
  }

  @Transactional
  fun createElectronicMonitoringProviderIfNotExists(licenceEntity: Licence) {
    if (licenceEntity is HasElectronicMonitoringResponseProvider) {
      licenceEntity.ensureElectronicMonitoringProviderExists()
    } else {
      error("ElectronicMonitoringProvider can only be initialized for licences that implement HasElectronicMonitorResponseProvider")
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
