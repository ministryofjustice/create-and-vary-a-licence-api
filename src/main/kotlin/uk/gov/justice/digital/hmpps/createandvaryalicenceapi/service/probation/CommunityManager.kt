package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class CommunityManager(
  val case: ProbationCase,
  val id: Long,
  val code: String,
  val name: Name,
  val provider: Detail,
  val team: TeamDetail,
  val allocationDate: LocalDate,
  val unallocated: Boolean,
  val email: String? = null,
  val telephoneNumber: String? = null,
)
