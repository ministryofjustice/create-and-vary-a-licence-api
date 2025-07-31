package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class UpdateStandardConditionDataRequest(
  @field:Schema(description = "The list of standard licence conditions from service configuration")
  @field:NotNull
  val standardLicenceConditions: List<StandardCondition> = emptyList(),

  @field:Schema(description = "The list of standard post sentence supervision conditions from service configuration")
  @field:NotNull
  val standardPssConditions: List<StandardCondition> = emptyList(),
)
