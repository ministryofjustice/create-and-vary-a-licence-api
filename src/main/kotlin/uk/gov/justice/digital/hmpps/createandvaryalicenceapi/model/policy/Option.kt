package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.policy

data class Option(
  val value: String,
  val conditional: Conditional? = null,
)
