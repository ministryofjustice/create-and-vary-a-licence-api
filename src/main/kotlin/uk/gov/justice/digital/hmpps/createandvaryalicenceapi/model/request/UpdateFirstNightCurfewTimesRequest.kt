package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.LocalTime

data class UpdateFirstNightCurfewTimesRequest(
  @field:Schema(description = "The first night curfew times for the licence")
  @field:NotNull
  val firstNightCurfewTimes: FirstNightCurfewTimeRequest,
)

@Schema(description = "Describes the first night curfew time on this hdc licence")
data class FirstNightCurfewTimeRequest(
  @field:Schema(description = "The time at which this curfew starts on the fromDay", example = "01:00:00")
  val fromTime: LocalTime? = null,

  @field:Schema(description = "The time at which this curfew ends on the untilDay", example = "01:00:00")
  val untilTime: LocalTime? = null,
)
