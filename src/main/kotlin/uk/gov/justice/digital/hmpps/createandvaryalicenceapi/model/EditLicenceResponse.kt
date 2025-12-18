package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "A reference to the edited licence")
data class EditLicenceResponse(
  @field:Schema(description = "Internal identifier for this licence edited within this service", example = "123344")
  val licenceId: Long,
)
