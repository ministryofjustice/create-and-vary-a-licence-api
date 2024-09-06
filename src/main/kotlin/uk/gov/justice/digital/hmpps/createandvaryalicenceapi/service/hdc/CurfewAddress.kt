package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

data class CurfewAddress(
  val addressLine1: String,
  val addressLine2: String? = null,
  val addressTown: String,
  val postCode: String,
)
