package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionPss(
  override var code: String,
  val category: String,
  val text: String,
  val tpl: String,
  val requiresInput: Boolean,
  val pssDates: Boolean?,
  val inputs: List<PassInput>,
) : ILicenceCondition
