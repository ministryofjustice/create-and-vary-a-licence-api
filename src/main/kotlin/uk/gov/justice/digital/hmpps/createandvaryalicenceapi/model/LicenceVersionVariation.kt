package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

class LicenceVersionVariation(
  stdApVariance: LicenceConditionChanges,
  stdPssVariation: LicenceConditionChanges,
  additionalApVariance: LicenceConditionChanges,
  additionalPssVariation: LicenceConditionChanges
) {
  val standardConditions = object {
    val ap = stdApVariance
    val pss = stdPssVariation
  }
  val additionalConditions = object {
    val ap = additionalApVariance
    val pss = additionalPssVariation
  }
}
