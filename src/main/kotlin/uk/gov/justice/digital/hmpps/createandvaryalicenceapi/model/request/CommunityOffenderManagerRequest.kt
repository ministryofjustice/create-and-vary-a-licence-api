package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Schema(description = "Request object a community offender manager")
data class CommunityOffenderManagerRequest(

  @Schema(description = "The username of the person who is creating the licence", example = "joebloggs")
  @field:NotBlank
  val username: String,

  @Schema(description = "The delius staff identifier of the person who is creating the licence", example = "014829475")
  @field:NotNull
  val staffIdentifier: Long,

  @Schema(description = "The email address of the person who is creating the licence", example = "joebloggs@probation.gov.uk")
  @field:NotBlank
  val email: String,
)
