package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import org.jetbrains.annotations.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Request object for overriding a licence status")
data class OverrideLicenceStatusRequest(
  @field:Schema(description = "The new status code to assign to the licence")
  @param:NotNull
  val statusCode: LicenceStatus,
  @field:Schema(description = "Reason for overriding the licence status")
  @param:NotEmpty
  val reason: String,
)
