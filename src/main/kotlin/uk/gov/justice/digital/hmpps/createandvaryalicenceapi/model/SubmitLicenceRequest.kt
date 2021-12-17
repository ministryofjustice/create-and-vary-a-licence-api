package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull

@Schema(description = "Request object for submitting a licence")
data class SubmitLicenceRequest(
  @Schema(description = "The username of the person who is updating this status", example = "ssmyth")
  @field:NotBlank
  val username: String,

  @Schema(description = "The DELIUS staff identifier of the person who is submitting status", example = "3000")
  @field:NotNull
  val staffIdentifier: Long,

  @Schema(description = "The first name of the person who is submitting the licence", example = "John")
  @field:NotBlank
  val firstName: String,

  @Schema(description = "The last name of the person who is submitting the licence", example = "Smythe")
  @field:NotBlank
  val surname: String,

  @Schema(description = "The email address of the person who is submitting the licence", example = "s.smyth@probation.gov.uk")
  val email: String,
)
