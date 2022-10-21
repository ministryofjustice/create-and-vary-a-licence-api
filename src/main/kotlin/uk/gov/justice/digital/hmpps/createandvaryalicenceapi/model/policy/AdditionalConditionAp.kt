package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionAp(
  override var code: String,
  override val category: String,
  override val text: String,
  override val tpl: String?,
  override val requiresInput: Boolean,
  override val inputs: List<Input>?,
  val categoryShort: String?,
  val subtext: String?,
  override val type: String?,
) : IAdditionalCondition<Input>
