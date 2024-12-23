package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty
import org.jetbrains.annotations.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

@Schema(description = "Request object for overriding a licence type")
data class OverrideLicenceTypeRequest(
  @Schema(description = "The new licence Type to assign to the licence")
  @NotNull
  val licenceType: LicenceType,
  @Schema(description = "Reason for overriding the licence status")
  @NotEmpty
  val reason: String,
)
