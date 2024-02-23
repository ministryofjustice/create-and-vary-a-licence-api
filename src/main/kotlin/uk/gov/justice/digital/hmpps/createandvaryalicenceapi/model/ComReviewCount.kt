package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.TeamCountsDto

@Schema(description = "Describes the counts of cases needed for review by a Probation Practitioner")
data class ComReviewCount(

  @Schema(description = "A count of cases that the probation practitioner needs to review", example = "42")
  val myCount: Long,

  @Schema(description = "A list of teams, the probation practitioner is attached and the count of cases that need review")
  val teams: List<TeamCountsDto>,
)
