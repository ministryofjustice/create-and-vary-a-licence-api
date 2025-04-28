package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class CaseloadResult(
  val crn: String,
  val nomisId: String? = null,
  val name: Name,
  val allocationDate: LocalDate,
  val staff: StaffDetail,
  val team: TeamDetail,
)
