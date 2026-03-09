package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes

data class UpdateCurfewTimesRequest(
  @field:Schema(description = "The list of hdc licence curfew times from service configuration")
  @field:NotNull
  val hdcWeeklyCurfewTimes: List<CurfewTimes> = emptyList(),
)
