package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class AdditionalConditionPss(
  override var code: String,
  override val category: String,
  override val text: String,
  override val tpl: String?,
  override val requiresInput: Boolean,
  override val categoryShort: String?,
  val pssDates: Boolean?,
  override val inputs: List<PssInput>?,
  override val type: String?,
) : IAdditionalCondition<PssInput>
