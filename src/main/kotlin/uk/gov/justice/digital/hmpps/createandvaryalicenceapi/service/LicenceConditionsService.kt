package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceConditionsRepository
import javax.persistence.EntityNotFoundException

data class ConditionChanges<T>(
  val removedConditions: List<T>,
  val addedConditions: List<T>,
  val amendedConditions: List<Pair<T, T>>
)

data class PolicyChanges<T, U>(val standardConditions: ConditionChanges<T>, val additionalConditions: ConditionChanges<U>)

@Service
class LicenceConditionsService(private val licenceRepository: LicenceConditionsRepository) {
  fun getAllConditions(): List<LicenceConditions> = licenceRepository.findAll()

  fun getConditionsByVersionList(versionList: List<Long>): List<LicenceConditions> =
    licenceRepository.findAllById(versionList)

  fun getConditionsByVersion(version: Long): LicenceConditions? = licenceRepository
    .findById(version)
    .orElseThrow { EntityNotFoundException("$version") }
}
