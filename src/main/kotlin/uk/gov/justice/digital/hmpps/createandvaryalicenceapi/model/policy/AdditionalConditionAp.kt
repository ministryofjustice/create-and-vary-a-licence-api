package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionAp(
  override var code: String,
  override val category: String,
  override val text: String,
  override val tpl: String? = null,
  override val requiresInput: Boolean,
  override val inputs: List<Input>? = null,
  override val categoryShort: String? = null,
  val subtext: String? = null,
  override val type: String? = null,
  override val skipable: Boolean? = null,
) : IAdditionalCondition
