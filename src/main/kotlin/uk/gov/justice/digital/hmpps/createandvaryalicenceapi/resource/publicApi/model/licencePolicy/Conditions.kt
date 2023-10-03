package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the conditions which form a licence policy")
data class Conditions(

  @Schema(description = "The list of standard conditions which form the licence policy")
  val standard: List<StandardConditionData>,

  @Schema(description = "The list of additional conditions which form the licence policy")
  val additional: List<AdditionalConditionData>,
)
