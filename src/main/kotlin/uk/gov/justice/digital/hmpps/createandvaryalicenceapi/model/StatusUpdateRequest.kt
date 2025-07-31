package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

@Schema(description = "Request object for updating the status of a licence")
data class StatusUpdateRequest(
  @field:Schema(description = "The new status for this licence", example = "APPROVED")
  @param:NotNull
  val status: LicenceStatus,

  @field:Schema(description = "The username of the person who is updating this status", example = "X12333")
  @field:NotBlank
  val username: String,

  @field:Schema(description = "The full name of the person who is updating this status", example = "John Smythe")
  val fullName: String? = null,
)
