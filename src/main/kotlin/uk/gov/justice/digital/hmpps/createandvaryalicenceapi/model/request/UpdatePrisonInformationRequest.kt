package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating the prison information on a licence")
data class UpdatePrisonInformationRequest(
  @Schema(description = "The identifier of the prison", example = "PVI")
  @field:NotBlank
  val prisonCode: String,

  @Schema(description = "The detailed name of the prison", example = "Pentonville (HMP)")
  @field:NotBlank
  val prisonDescription: String,

  @Schema(description = "The prison telephone number", example = "+44 276 54545")
  val prisonTelephone: String? = null,
)
