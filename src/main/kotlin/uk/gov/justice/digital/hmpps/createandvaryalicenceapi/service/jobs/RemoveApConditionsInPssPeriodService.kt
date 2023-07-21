package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun removeAPConditions() {
    log.info("Job to removeApConditions in PSS period started")
    val licencesInPSSPeriod = licenceRepository.getAllVariedLicencesInPSSPeriod()

    licencesInPSSPeriod.forEach { licence ->
      val aPConditionIds =
        licence.additionalConditions.filter { LicenceType.valueOf(it.conditionType!!) == LicenceType.AP }.map { it.id }
      this.licenceConditionService.deleteAdditionalConditions(licence, aPConditionIds)
    }
    log.info("Job removeApConditions in PSS period deleted AP conditions on ${licencesInPSSPeriod.size} licences")
  }
}
