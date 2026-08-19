package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class ManagedOffender(
  val crn: String,
  val nomisId: String? = null,
  val name: Name,
  val allocationDate: LocalDate? = null,
  val staff: StaffDetail,
  val team: TeamSummary? = null,
)

data class TeamSummary(
  val code: String,
  val description: String,
)
