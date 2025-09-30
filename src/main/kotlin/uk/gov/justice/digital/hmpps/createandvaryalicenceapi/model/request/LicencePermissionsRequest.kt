package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

@Schema(description = "Request object for checking what access a user has to a licence")
data class LicencePermissionsRequest(

  @field:Schema(
    description = "For a COM user, the teams they are allocated to",
    example = "['team-A', 'team-S']",
  )
  @field:NotEmpty
  val teamCodes: List<String>,
)
