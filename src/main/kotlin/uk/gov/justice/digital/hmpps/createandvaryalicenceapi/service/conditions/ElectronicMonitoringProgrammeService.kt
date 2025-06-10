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

  fun handleElectronicMonitoringResponseRecords(licenceEntity: Licence) {
    val additionalApConditions = licenceEntity.additionalConditions.filter { it.conditionType == "AP" }
    val conditionsRequiringElectronicMonitoringResponse = licencePolicyService.getConditionsRequiringElectronicMonitoringResponse(licenceEntity.version!!, additionalApConditions.map { it.conditionCode })
    if (conditionsRequiringElectronicMonitoringResponse.isNotEmpty()) {
      LicenceConditionService.log.info("Handling Electronic Monitoring response record for conditions: ${conditionsRequiringElectronicMonitoringResponse.joinToString(",") { it.code }}")
      initialiseElectronicMonitoringProviderIfNotExists(licenceEntity)
    } else {
      clearElectronicMonitoringProvider(licenceEntity)
    }
  }

  private fun clearElectronicMonitoringProvider(licenceEntity: Licence) {
    LicenceConditionService.log.info("Clearing Electronic Monitoring response records for licence: ${licenceEntity.id}")
    when (licenceEntity) {
      is CrdLicence -> licenceEntity.electronicMonitoringProvider = null
      is HdcLicence -> licenceEntity.electronicMonitoringProvider = null
      else -> error("ElectronicMonitoringProvider can only be cleared for CrdLicence or HdcLicence")
    }
  }

  private fun initialiseElectronicMonitoringProviderIfNotExists(licenceEntity: Licence) {
    when (licenceEntity) {
      is CrdLicence -> {
        if (licenceEntity.electronicMonitoringProvider == null) {
          licenceEntity.electronicMonitoringProvider = ElectronicMonitoringProvider(
            licence = licenceEntity,
            isToBeTaggedForProgramme = null,
            programmeName = null,
          )
        }
      }
      is HdcLicence -> {
        if (licenceEntity.electronicMonitoringProvider == null) {
          licenceEntity.electronicMonitoringProvider = ElectronicMonitoringProvider(
            licence = licenceEntity,
            isToBeTaggedForProgramme = null,
            programmeName = null,
          )
        }
      }
      else -> error("ElectronicMonitoringProvider can only be initialized for CrdLicence or HdcLicence")
    }
  }

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
