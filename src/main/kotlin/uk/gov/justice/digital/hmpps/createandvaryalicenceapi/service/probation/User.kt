package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class User(
  val email: String? = null,
  val username: String?,
  val staffIdentifier: Long?,
  val staff: StaffHuman? = null,
  val teams: List<Detail>,
  val staffCode: String? = null,
)

data class StaffHuman(
  val code: String? = null,
  val forenames: String?,
  val surname: String?,
  val unallocated: Boolean? = null,
)
