package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class StaffDetail(
  val code: String,
  val name: Name?,
  val unallocated: Boolean? = false,
)
