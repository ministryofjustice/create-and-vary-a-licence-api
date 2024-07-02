package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation

class ProbationStatus(
  val awaitingPsr: Boolean,
  val inBreach: Boolean?,
  val preSentenceActivity: Boolean,
  val previouslyKnownTerminationDate: String?,
  val status: String,
)
