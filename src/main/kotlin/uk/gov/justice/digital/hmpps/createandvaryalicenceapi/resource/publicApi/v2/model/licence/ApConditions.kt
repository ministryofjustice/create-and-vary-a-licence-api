package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersion
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.latestApiVersionPostFix
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licence.BespokeCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.model.licencePolicy.StandardCondition
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.resource.publicApi.v2.model.licence.additionalConditions.AdditionalCondition

@Schema(
  name = "ApConditions$latestApiVersionPostFix",
  description = "Describes the All Purpose conditions which apply to a licence, $latestApiVersion",
)
data class ApConditions(
  @field:Schema(description = "The list of standard conditions which form the licence")
  val standard: List<StandardCondition>,

  @field:Schema(description = "The list of additional conditions which form the licence")
  val additional: List<AdditionalCondition>,

  @field:Schema(description = "The list of bespoke conditions which form the licence")
  val bespoke: List<BespokeCondition>,
)
