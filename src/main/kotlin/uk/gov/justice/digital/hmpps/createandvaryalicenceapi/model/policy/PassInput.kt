package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class PassInput(
  val type: String,
  val label: String,
  val name: String,
  val includeBefore: String?,
)
