package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class ManagedOffenderCrn(
  val allocationDate: String? = null,
  val offenderCrn: String? = null,
  val staff: StaffDetail? = null,
  val staffIdentifier: Long? = null,
  val team: Team? = null,
  val teamIdentifier: Long? = null,
)
