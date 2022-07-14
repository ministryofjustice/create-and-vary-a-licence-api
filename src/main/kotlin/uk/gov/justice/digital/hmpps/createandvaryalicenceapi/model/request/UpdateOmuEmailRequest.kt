package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Request object for updating / creating OMU email contact")
class UpdateOmuEmailRequest(
  @Schema(description = "The email used to contact the OMU", example = "test@omu.prison.com")
  val email: String,
)
