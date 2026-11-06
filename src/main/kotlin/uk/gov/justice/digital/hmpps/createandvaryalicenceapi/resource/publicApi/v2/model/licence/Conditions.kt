package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix

@Schema(
  name = "Conditions$latestApiVersionPostFix",
  description = "The AP and PSS conditions on a licence, $latestApiVersion",
)
data class Conditions(
  @field:JsonProperty("AP")
  @field:Schema(description = "The AP conditions that form the licence")
  val apConditions: ApConditions,

  @field:JsonProperty("PSS")
  @field:Schema(description = "The PSS conditions that form the licence")
  val pssConditions: PssConditions,
)
