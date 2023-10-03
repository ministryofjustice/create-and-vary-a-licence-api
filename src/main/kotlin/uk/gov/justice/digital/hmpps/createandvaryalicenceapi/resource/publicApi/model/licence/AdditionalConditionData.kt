package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Describes each additional condition on the licence")
data class AdditionalConditionData(

  @Schema(description = "The additional condition's unique code", example = "abcd-1234-efgh")
  val code: String,

  @Schema(description = "The category to which the additional condition belongs", example = "Residence at a specific place")
  val category: String,

  @Schema(description = "The instances of an additional condition where multiple versions of the same condition exist")
  val instances: List<AdditionalConditionInstance>,

)
