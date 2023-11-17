package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionPss(
  override var code: String,
  override val category: String,
  override val text: String,
  override val tpl: String? = null,
  override val requiresInput: Boolean,
  override val categoryShort: String? = null,
  val pssDates: Boolean? = null,
  override val inputs: List<Input>? = null,
  override val type: String? = null,
  override val skippable: Boolean? = false,
) : IAdditionalCondition
