package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes a type of condition on a licence policy")
data class ConditionTypes(

  @JsonProperty("AP")
  @Schema(description = "The AP conditions that form part of the licence policy")
  val apConditions: LicencePolicyConditions,

  @JsonProperty("PSS")
  @Schema(description = "The PSS conditions that form part of the licence policy")
  val pssConditions: LicencePolicyConditions,
)
