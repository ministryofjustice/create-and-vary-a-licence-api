package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes

data class UpdateFirstNightCurfewTimesRequest(
  @field:Schema(description = "The first night curfew times for the licence")
  @field:NotNull
  val firstNightCurfewTimes: CurfewTimes,
)
