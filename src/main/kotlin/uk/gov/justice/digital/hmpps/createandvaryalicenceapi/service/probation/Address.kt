package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class Address(
  val addressNumber: String?,
  val buildingName: String?,
  val county: String?,
  val district: String?,
  val from: String,
  val noFixedAbode: Boolean?,
  val notes: String?,
  val postcode: String?,
  val status: KeyValue,
  val streetName: String?,
  val telephoneNumber: String?,
  val to: String?,
  val town: String?,
)
