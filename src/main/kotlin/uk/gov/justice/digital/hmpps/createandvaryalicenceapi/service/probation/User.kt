package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class User(
  val email: String?,
  val username: String?,
  val staffIdentifier: Long?,
  val staff: StaffHuman?,
  val teams: List<Detail>,
)

data class StaffHuman(
  val code: String,
  val forenames: String?,
  val surname: String?,
)
