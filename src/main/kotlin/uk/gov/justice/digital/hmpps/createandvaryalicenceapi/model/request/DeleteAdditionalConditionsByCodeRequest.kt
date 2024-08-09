package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A list of licence condition codes to be removed from a licence")
data class DeleteAdditionalConditionsByCodeRequest(
  @Schema(description = "List of condition codes", example = "a1a1a1a1-b2b2-c3c3-d4d4-e5e5e5e5e5e5")
  val conditionCodes: List<String>
)
