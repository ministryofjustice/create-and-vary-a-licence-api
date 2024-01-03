package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison

data class PhoneDetail(
  val phoneId: Int,
  val number: String,
  val type: String,
  val ext: String?,
)
