package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The type of conditions on a licence policy which can be AP (All Purpose) and/or PSS (Post Sentence Supervision)")
data class ConditionTypes(

  @JsonProperty("AP")
  @Schema(description = "The AP conditions that form part of the licence policy")
  val apConditions: LicencePolicyConditionsAp,

  @JsonProperty("PSS")
  @Schema(description = "The PSS conditions that form part of the licence policy")
  val pssConditions: LicencePolicyConditionsPss,
)
