package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

data class ProbationCase(
  val crn: String,
  val nomisId: String? = null,
  val croNumber: String? = null,
  val pncNumber: String? = null,
)
