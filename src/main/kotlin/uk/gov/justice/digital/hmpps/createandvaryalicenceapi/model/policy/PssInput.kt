package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class PssInput(
  val type: String,
  val label: String,
  val name: String,
  val includeBefore: String?,
)
