package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Response object containing permissions a user has for a licence")
data class LicencePermissionsResponse(

  @field:Schema(
    description = "If true then the user can view the licence",
    example = "true",
  )
  val view: Boolean,
)
