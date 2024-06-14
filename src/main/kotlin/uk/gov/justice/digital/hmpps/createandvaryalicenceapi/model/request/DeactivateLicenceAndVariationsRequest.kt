package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.DateChangeLicenceDeativationReason

@Schema(description = "Request object for deactivating an active licence and its variations")
data class DeactivateLicenceAndVariationsRequest(
  @Schema(description = "A key representing the reason for the variation", example = "RESENTENCED")
  val reason: DateChangeLicenceDeativationReason,
)
