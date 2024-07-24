package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate

data class ManagedOffenderCrn(
  val offenderCrn: String? = null,
  val allocationDate: LocalDate? = null,
  val staffIdentifier: Long? = null,
  val staff: StaffHuman? = null,
  val teamIdentifier: Long? = null,
  val team: TeamDetail? = null,
)
