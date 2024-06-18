package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

import java.time.LocalDate
import java.time.LocalDateTime

data class ApprovalCase(
  val licenceId: Long?,
  val name: String?,
  val prisonerNumber: String?,
  val submittedByFullName: String?,
  val releaseDate: LocalDate?,
  val sortDate: LocalDate?,
  val urgentApproval: Boolean?,
  val approvedBy: String?,
  val approvedOn: LocalDateTime?,
  val isDueForEarlyRelease: Boolean?,
  val probationPractitioner: ProbationPractitioner?,
)
