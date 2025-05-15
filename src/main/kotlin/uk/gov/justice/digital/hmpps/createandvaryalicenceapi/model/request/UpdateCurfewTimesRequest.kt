package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes

data class UpdateCurfewTimesRequest(
  @Schema(description = "The list of hdc licence curfew times from service configuration")
  @field:NotNull
  val curfewTimes: List<HdcCurfewTimes> = emptyList(),
)
