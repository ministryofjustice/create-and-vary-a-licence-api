package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request object for updating / creating OMU email contact")
data class UpdateOmuEmailRequest(
  @Schema(description = "The email used to contact the OMU", example = "test@omu.prison.com")
  @field:NotBlank
  @field:Email
  val email: String,
)
