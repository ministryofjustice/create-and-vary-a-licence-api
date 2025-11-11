package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider
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
) {

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
  fun createMonitoringProviderIfRequired(licenceEntity: Licence, conditionCodes: Set<String>) {
    val version = requireNotNull(licenceEntity.version) { "Licence version cannot be null" }
    val isResponseRequired = licencePolicyService.isElectronicMonitoringResponseRequired(conditionCodes, version)
    if (isResponseRequired) {
      createElectronicMonitoringProviderIfNotExists(licenceEntity)
    }
  }

  @Transactional
  fun removeMonitoringProviderIfNoLongerRequired(licenceEntity: Licence, removedConditionCodes: Set<String>) {
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

    check(licenceEntity is HasElectronicMonitoringResponseProvider) {
      "ElectronicMonitoringProvider can only be deleted for licences that implement HasElectronicMonitoringResponseProvider"
    }

    when (licenceEntity) {
      is PrrdLicence -> licenceEntity.electronicMonitoringProvider = null
      is CrdLicence -> licenceEntity.electronicMonitoringProvider = null
      is HdcLicence -> licenceEntity.electronicMonitoringProvider = null
    }
  }

  private fun createElectronicMonitoringProviderIfNotExists(licenceEntity: Licence) {
    check(licenceEntity is HasElectronicMonitoringResponseProvider) {
      "ElectronicMonitoringProvider can only be initialized for licences that implement HasElectronicMonitoringResponseProvider"
    }

    licenceEntity.ensureProviderExists()
  }

  private fun HasElectronicMonitoringResponseProvider.hasProvider(): Boolean = when (this) {
    is PrrdLicence -> this.electronicMonitoringProvider != null
    is CrdLicence -> this.electronicMonitoringProvider != null
    is HdcLicence -> this.electronicMonitoringProvider != null
    else -> false
  }

  private fun HasElectronicMonitoringResponseProvider.ensureProviderExists() {
    if (!this.hasProvider()) {
      when (this) {
        is PrrdLicence -> this.electronicMonitoringProvider = ElectronicMonitoringProvider(licence = this)
        is CrdLicence -> this.electronicMonitoringProvider = ElectronicMonitoringProvider(licence = this)
        is HdcLicence -> this.electronicMonitoringProvider = ElectronicMonitoringProvider(licence = this)
      }
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
