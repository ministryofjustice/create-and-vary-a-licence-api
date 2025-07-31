package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.additionalConditions.AdditionalCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition

@Schema(description = "Describes the All Purpose conditions which apply to a licence")
data class ApConditions(

  @field:Schema(description = "The list of standard conditions which form the licence")
  val standard: List<StandardCondition>,

  @field:Schema(description = "The list of additional conditions which form the licence")
  val additional: List<AdditionalCondition>,

  @field:Schema(description = "The list of bespoke conditions which form the licence")
  val bespoke: List<BespokeCondition>,
)
