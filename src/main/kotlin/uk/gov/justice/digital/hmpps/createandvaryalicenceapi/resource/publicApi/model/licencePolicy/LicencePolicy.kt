package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes the set of conditions associated with a given version of the policy")
data class LicencePolicy(

  @Schema(description = "The version of the licence policy", example = "2.1")
  val version: String,

  @Schema(description = "The AP and PSS conditions that form the licence policy")
  val conditions: ConditionTypes,
)
