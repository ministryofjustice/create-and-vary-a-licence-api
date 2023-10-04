package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardConditionData

@Schema(description = "Describes the AP conditions which apply to a licence")
data class ApConditions(

  @Schema(description = "The list of standard conditions which form the licence")
  val standard: List<StandardConditionData>,

  @Schema(description = "The list of additional conditions which form the licence")
  val additional: List<AdditionalConditionData>,

  @Schema(description = "The list of bespoke conditions which form the licence")
  val bespoke: List<BespokeConditionData>,
)