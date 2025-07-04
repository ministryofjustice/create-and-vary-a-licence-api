package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.conditions

import jakarta.persistence.EntityNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateElectronicMonitoringProgrammeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind

@Service
class ElectronicMonitoringProgrammeService(
  private val licenceRepository: LicenceRepository,
  private val licencePolicyService: LicencePolicyService,
  private val auditService: AuditService,
  private val staffRepository: StaffRepository,
  @Value("\${feature.toggle.electronicMonitoringResponseHandling:false}")
  private val electronicMonitoringResponseHandlingEnabled: Boolean = false,
) {

  @Transactional
  fun handleResponseIfEnabled(kind: LicenceKind, conditionCodes: Set<String>) {
    if (electronicMonitoringResponseHandlingEnabled && (LicenceKind.CRD, Licencekidn || licence is HdcLicence)) {
      handleElectronicMonitoringResponseRecords(licence)
    }
  }

  // if no EM conditions remain, then remove if present
  fun handleRemovedCondtionsIfEnabled(kind: LicenceKind, toSet: Set<String>) {}


  @Transactional
  fun updateElectronicMonitoringProgramme(licenceId: Long, request: UpdateElectronicMonitoringProgrammeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val electronicMonitoringProvider = when (licenceEntity) {
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
  fun handleElectronicMonitoringResponseRecords(licenceEntity: Licence) {
    val isResponseRequired = licencePolicyService.isElectronicMonitoringResponseRequired(licenceEntity)
    if (isResponseRequired) {
      createElectronicMonitoringProviderIfNotExists(licenceEntity)
    } else {
      deleteElectronicMonitoringProvider(licenceEntity)
    }
  }

  @Transactional
  fun deleteElectronicMonitoringProvider(licenceEntity: Licence) {
    log.info("Clearing Electronic Monitoring response records for licence: ${licenceEntity.id}")
    when (licenceEntity) {
      is CrdLicence -> licenceEntity.electronicMonitoringProvider = null
      is HdcLicence -> licenceEntity.electronicMonitoringProvider = null
    }
  }

  @Transactional
  fun createElectronicMonitoringProviderIfNotExists(licenceEntity: Licence) {
    when (licenceEntity) {
      is CrdLicence -> {
        if (licenceEntity.electronicMonitoringProvider == null) {
          licenceEntity.electronicMonitoringProvider = createNewElectronicMonitoringProvider(licenceEntity)
        }
      }
      is HdcLicence -> {
        if (licenceEntity.electronicMonitoringProvider == null) {
          licenceEntity.electronicMonitoringProvider = createNewElectronicMonitoringProvider(licenceEntity)
        }
      }
      else -> error("ElectronicMonitoringProvider can only be initialized for CrdLicence or HdcLicence")
    }
  }

  private fun createNewElectronicMonitoringProvider(licenceEntity: Licence): ElectronicMonitoringProvider = ElectronicMonitoringProvider(
    licence = licenceEntity,
    isToBeTaggedForProgramme = null,
    programmeName = null,
  )


  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
