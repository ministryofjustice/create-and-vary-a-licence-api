package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class User(
  val id: Long,
  val code: String,
  val name: Name,
  val teams: List<TeamDetail> = listOf(),
  val provider: Detail,
  val username: String? = null,
  val email: String? = null,
  val telephoneNumber: String? = null,
  val unallocated: Boolean,
)
