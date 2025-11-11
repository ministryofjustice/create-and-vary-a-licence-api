package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a team and the respective count of their cases that need review")
data class TeamCountsDto(
  @field:Schema(description = "The team code", example = "ABC123")
  val teamCode: String,

  @field:Schema(description = "A count of cases that need to be reviewed", example = "42")
  val count: Long,
)
