package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class Conditional(
  val inputs: List<ConditionalInput>,
) : HasInputs {
  override fun getConditionInputs() = inputs.map { it.toInput() }
}
