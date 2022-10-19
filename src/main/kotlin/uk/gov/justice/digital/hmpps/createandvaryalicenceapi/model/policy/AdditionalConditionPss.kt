package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionPss(
  override var code: String,
  val category: String,
  override val text: String,
  override val tpl: String?,
  override val requiresInput: Boolean,
  val pssDates: Boolean?,
  val inputs: List<PassInput>?,
  val type: String?,
) : ILicenceCondition
