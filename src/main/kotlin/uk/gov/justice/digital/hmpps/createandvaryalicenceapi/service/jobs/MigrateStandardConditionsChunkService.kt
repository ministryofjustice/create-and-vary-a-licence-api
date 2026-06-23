package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.AuditService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.policies.LicencePolicyService

@Service
class MigrateStandardConditionsChunkService(
  private val auditService: AuditService,
  private val licencePolicyService: LicencePolicyService,
  private val licenceRepository: LicenceRepository,
) {
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun migrateStandardConditions(licenceIds: List<Long>, policyVersion: String) {
    if (licenceIds.isEmpty()) return

    val licences = licenceRepository.findAllById(licenceIds)
    licences.forEach { licence ->
      val standardConditions = licence.standardConditions
      if (standardConditions.isEmpty() || standardConditions.any { it.conditionVersion != policyVersion }) {
        this.updateStandardConditionsToVersion(licence, policyVersion)
      }
    }
  }

  private fun updateStandardConditionsToVersion(licence: Licence, policyVersion: String) {
    val newConditions = licencePolicyService.getStandardConditionsForLicence(licence, policyVersion)
    licence.updateConditions(
      updatedStandardConditions = newConditions,
      staffMember = null,
    )

    licenceRepository.saveAndFlush(licence)
    auditService.recordAuditEventUpdateStandardCondition(licence, policyVersion, null)
  }
}
