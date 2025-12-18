package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A reference to the created variation licence")
data class CreateVariationResponse(
  @field:Schema(description = "Internal identifier for this licence generated within this service", example = "123344")
  val licenceId: Long,
)
