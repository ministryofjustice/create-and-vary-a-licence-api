package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class User(
  val id: Long? = null,
  val code: String? = null,
  val name: Name? = null,
  val teams: List<Detail>? = null,
  val username: String?,
  val email: String? = null,
  val unallocated: Boolean? = null,
)



