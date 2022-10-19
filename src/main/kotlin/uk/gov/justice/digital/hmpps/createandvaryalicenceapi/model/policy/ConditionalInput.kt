package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class ConditionalInput(
  val type: String,
  val label: String,
  val name: String,
  val case: String?,
  val handleIndefiniteArticle: Boolean?,
  val includeBefore: String?,
  val subtext: String?,
)
