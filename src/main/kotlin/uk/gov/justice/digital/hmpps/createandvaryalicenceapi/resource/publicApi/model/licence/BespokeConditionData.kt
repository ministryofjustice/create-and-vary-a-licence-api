package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each bespoke condition on the licence")
data class BespokeConditionData(

  @Schema(description = "The text for the bespoke condition", example = "You should not visit Y")
  val text: String,
)
