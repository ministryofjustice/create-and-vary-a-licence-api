package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import org.jetbrains.annotations.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Request object for overriding a licence status")
data class OverrideLicenceStatusRequest(
  @Schema(description = "The new status code to assign to the licence")
  @NotNull
  val statusCode: LicenceStatus,
  @Schema(description = "Reason for overriding the licence status")
  @NotEmpty
  val reason: String
)
