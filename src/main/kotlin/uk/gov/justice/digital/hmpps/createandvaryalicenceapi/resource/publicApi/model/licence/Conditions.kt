package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
@Schema(description = "The type of conditions on a licence which can be AP (All Purpose) and/or PSS (Post Sentence Supervision)")
data class Conditions(

  @JsonProperty("AP")
  @Schema(description = "The AP conditions that form the licence")
  val apConditions: ApConditions,

  @JsonProperty("PSS")
  @Schema(description = "The PSS conditions that form the licence")
  val pssConditions: PssConditions,
)
