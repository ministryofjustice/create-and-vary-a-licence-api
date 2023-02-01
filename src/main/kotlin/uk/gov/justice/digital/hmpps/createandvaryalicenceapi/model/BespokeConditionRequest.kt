package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "A list of bespoke conditions to add to a licence")
data class BespokeConditionRequest(
  @Schema(description = "A list of bespoke conditions to add to a licence", example = "['cond1', 'cond2']")
  @field:NotNull
  val conditions: List<String> = emptyList()
)
