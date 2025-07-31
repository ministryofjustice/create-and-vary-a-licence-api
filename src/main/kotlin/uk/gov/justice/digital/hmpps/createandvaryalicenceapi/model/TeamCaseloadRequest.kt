package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for requesting a case load for a team")
data class TeamCaseloadRequest(
  @field:Schema(description = "The probation team codes to get the case loads for", example = "[\"teamA\", \"teamC\"]")
  val probationTeamCodes: List<String> = emptyList(),

  @field:Schema(
    description = "The teams linked to the user to get the case loads for",
    example = "[\"teamA\", \"teamC\"]",
  )
  val teamSelected: List<String> = emptyList(),
)
