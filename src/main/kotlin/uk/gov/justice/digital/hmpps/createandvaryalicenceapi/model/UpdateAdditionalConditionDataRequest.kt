package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotNull

@Schema(description = "Request object for updating the list of additional conditions on a licence")
data class UpdateAdditionalConditionDataRequest(
  @Schema(description = "The list of data inputs associated with this additional condition")
  @field:NotNull
  val data: List<AdditionalCondition>,
)
