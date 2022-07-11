package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ILicenceCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceConditions
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceConditionsDto
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ConditionVariations
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceConditionChanges
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.LicenceVersionVariation
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceConditionsRepository
import javax.persistence.EntityNotFoundException

@Service
class LicenceConditionsService(private val licenceRepository: LicenceConditionsRepository) {
  fun getAllConditions(): List<LicenceConditions> = licenceRepository.findAll()

  fun getConditionsByVersionList(versionList: List<Long>): List<LicenceConditions> =
    licenceRepository.findAllById(versionList)

  fun getConditionsByVersion(version: Long): LicenceConditions? = licenceRepository
    .findById(version)
    .orElseThrow { EntityNotFoundException("$version") }

  fun compareLicenceConditions(
    currentVersion: LicenceConditionsDto,
    otherVersion: LicenceConditionsDto
  ): LicenceVersionVariation = LicenceConditionsComparator(currentVersion, otherVersion).compare()
}

class LicenceConditionsComparator(private val currentVersion: LicenceConditionsDto, private val otherVersion: LicenceConditionsDto) {

  fun getRemovedConditions(current: List<ILicenceCondition>, other: List<ILicenceCondition>): List<ILicenceCondition> =
    other.filter { pssElement ->
      current.findLast { pss -> pss.code == pssElement.code } == null
    }

  fun getAddedConditions(current: List<ILicenceCondition>, other: List<ILicenceCondition>): List<ILicenceCondition> =
    current.filter { pssElement ->
      other.findLast { pss -> pss.code == pssElement.code } == null
    }

  fun getAmendedConditions(
    current: List<ILicenceCondition>,
    other: List<ILicenceCondition>
  ): List<ConditionVariations?> =
    current.filter { currentCondition ->
      other.find {
        it.code == currentCondition.code && it.hashCode() != currentCondition.hashCode()
      } != null
    }.map { currentCondition ->
      other.find { it.code == currentCondition.code }?.let { ConditionVariations(it, currentCondition) }
    }

  fun compare(): LicenceVersionVariation {
    val stdPssVariance = LicenceConditionChanges(
      getRemovedConditions(currentVersion.standardConditions.pss, otherVersion.standardConditions.pss),
      getAddedConditions(currentVersion.standardConditions.pss, otherVersion.standardConditions.pss),
      getAmendedConditions(currentVersion.standardConditions.pss, otherVersion.standardConditions.pss)
    )

    val stdApVariance = LicenceConditionChanges(
      getRemovedConditions(currentVersion.standardConditions.ap, otherVersion.standardConditions.ap),
      getAddedConditions(currentVersion.standardConditions.ap, otherVersion.standardConditions.ap),
      getAmendedConditions(currentVersion.standardConditions.ap, otherVersion.standardConditions.ap)
    )

    val additionalPssVariance = LicenceConditionChanges(
      getRemovedConditions(currentVersion.additionalConditions.pss, otherVersion.additionalConditions.pss),
      getAddedConditions(currentVersion.additionalConditions.pss, otherVersion.additionalConditions.pss),
      getAmendedConditions(currentVersion.additionalConditions.pss, otherVersion.additionalConditions.pss)
    )

    val additionalApVariance = LicenceConditionChanges(
      getRemovedConditions(currentVersion.additionalConditions.ap, otherVersion.additionalConditions.ap),
      getAddedConditions(currentVersion.additionalConditions.ap, otherVersion.additionalConditions.ap),
      getAmendedConditions(currentVersion.additionalConditions.ap, otherVersion.additionalConditions.ap)
    )

    return LicenceVersionVariation(stdApVariance, stdPssVariance, additionalApVariance, additionalPssVariance)
  }
}
