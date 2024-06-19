package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class StaffDetail(
  val code: String,
  val forenames: String?,
  val surname: String?,
  val unallocated: Boolean? = false,
)
