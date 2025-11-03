package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import software.amazon.awssdk.annotations.NotNull

@Schema(description = "Request object for updating a reason for NOMIS Time Served licence")
data class UpdateNomisLicenceReasonRequest(
  @field:Schema(description = "The updated reason for creating the licence in NOMIS", example = "Updated reason: time served release adjustment")
  @field:NotNull
  val reason: String,
)
