package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceConditionService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Service
class RemoveExpiredConditionsService(
  private val licenceRepository: LicenceRepository,
  private val licenceConditionService: LicenceConditionService,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  @Transactional
  fun removeExpiredConditions() {
    log.info("Job to removeExpiredConditions in PSS period started")
    val licencesInPSSPeriod = licenceRepository.getAllVariedLicencesInPSSPeriod()
    log.info("Job removeExpiredConditions found ${licencesInPSSPeriod.size} licences to process")

    licencesInPSSPeriod.forEach { licence ->
      val additionalApConditionIds =
        licence.additionalConditions.filter { LicenceType.valueOf(it.conditionType!!) == LicenceType.AP }.map { it.id }
      val standardApConditionIds =
        licence.standardConditions.filter { LicenceType.valueOf(it.conditionType!!) == LicenceType.AP }.map { it.id }
      val bespokeConditionIds = licence.bespokeConditions.map { it.id }
      this.licenceConditionService.deleteConditions(
        licence,
        additionalApConditionIds,
        standardApConditionIds,
        bespokeConditionIds,
      )
    }
    log.info("Job removeExpiredConditions in PSS period deleted AP and Bespoke conditions on ${licencesInPSSPeriod.size} licences")
  }
}
