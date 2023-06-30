package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceConditionService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class RemoveApConditionsInPssPeriodService(
  private val licenceRepository: LicenceRepository,
  private val licenceConditionService: LicenceConditionService,
) {
  @Transactional
  fun removeAPConditions() {
    val licencesInPSSPeriod = licenceRepository.getAllVariedLicencesInPSSPeriod()

    licencesInPSSPeriod?.forEach { licence ->
      val aPConditionIds = licence.additionalConditions.filter { LicenceType.valueOf(it.conditionType!!) == LicenceType.AP }.map { it.id }
      this.licenceConditionService.deleteAdditionalConditions(licence.id, aPConditionIds)
    }
  }
}
