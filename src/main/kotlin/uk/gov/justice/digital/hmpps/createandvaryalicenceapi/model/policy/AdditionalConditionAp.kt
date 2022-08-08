package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionAp(
  override var code: String,
  val category: String,
  val text: String,
  val tpl: String?,
  val requiresInput: Boolean,
  val inputs: List<Input>?,
  val categoryShort: String?,
  val subtext: String?,
) : ILicenceCondition
