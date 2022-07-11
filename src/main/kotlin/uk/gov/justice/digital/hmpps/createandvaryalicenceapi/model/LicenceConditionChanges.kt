package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ILicenceCondition

data class LicenceConditionChanges(
  val removed: List<ILicenceCondition>,
  val added: List<ILicenceCondition>,
  val changed: List<ConditionVariations?>
)
