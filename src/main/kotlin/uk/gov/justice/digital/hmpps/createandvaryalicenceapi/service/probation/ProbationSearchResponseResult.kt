package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class ProbationSearchResponseResult(
  val name: Name,
  val identifiers: Identifiers,
  val manager: Manager,
  val allocationDate: String,
)
