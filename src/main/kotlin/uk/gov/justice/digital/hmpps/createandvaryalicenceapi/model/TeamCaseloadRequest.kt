package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

data class TeamCaseloadRequest(
  val probationTeamCodes: List<String>,
  val teamSelected: List<String>?,
)
