package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@Schema(description = "Request object for updating the list of additional conditions on a licence")
data class AdditionalConditionsRequest(
  @Schema(description = "The list of additional conditions")
  @field:NotNull
  val additionalConditions: List<AdditionalCondition>,

  @Schema(description = "The type of additional condition, either licence or post sentence supervision", allowableValues = ["AP", "PSS"])
  @field:NotNull
  val conditionType: String
)
