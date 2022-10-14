package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionAp(
  override var code: String,
  val category: String,
  override val text: String,
  override val tpl: String?,
  override val requiresInput: Boolean,
  val inputs: List<Input>?,
  val categoryShort: String?,
  val subtext: String?,
  val type: String?,
) : ILicenceCondition
