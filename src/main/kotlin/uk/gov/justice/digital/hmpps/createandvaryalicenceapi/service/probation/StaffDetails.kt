package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class StaffDetails(
  val username: String,
  val email: String?,
  val telephoneNumber: String?,
  val staffCode: String?,
  val staffIdentifier: Long?,
  val staff: StaffHuman,
  val teams: TeamDetail,
  val probationArea: ProbationAreaDetail,
)
