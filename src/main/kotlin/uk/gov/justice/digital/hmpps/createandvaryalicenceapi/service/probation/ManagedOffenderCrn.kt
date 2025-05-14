package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class ManagedOffenderCrn(
  val crn: String? = null,
  val nomisId: String? = null,
  val name: Name? = null,
  val allocationDate: LocalDate? = null,
  val staff: StaffDetail? = null,
  val team: TeamDetail? = null,
)
