package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class ManagedOffenderCrn(
  val crn: String,
  val nomisId: String? = null,
  val name: Name,
  val allocationDate: LocalDate? = null,
  val staff: StaffDetail,
  val team: TeamDetail? = null,
)
