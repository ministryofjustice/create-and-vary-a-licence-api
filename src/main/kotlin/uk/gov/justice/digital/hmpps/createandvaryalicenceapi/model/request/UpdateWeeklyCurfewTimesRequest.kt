package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import java.time.DayOfWeek
import java.time.LocalTime

data class UpdateWeeklyCurfewTimesRequest(
  @field:Schema(description = "The list of hdc licence curfew times from service configuration")
  @field:NotNull
  val weeklyCurfewTimes: List<CurfewTimeRequest> = emptyList(),
)

@Schema(description = "Describes the curfew times on this hdc licence")
data class CurfewTimeRequest(

  @field:Schema(description = "Sequence of this curfew time within the curfew times", example = "1")
  val curfewTimesSequence: Int? = null,

  @field:Schema(description = "The day on which this curfew starts for this curfew time", example = "MONDAY")
  val fromDay: DayOfWeek? = null,

  @field:Schema(description = "The time at which this curfew starts on the fromDay", example = "01:00:00")
  val fromTime: LocalTime? = null,

  @field:Schema(description = "The day on which this curfew ends for this curfew time", example = "MONDAY")
  val untilDay: DayOfWeek? = null,

  @field:Schema(description = "The time at which this curfew ends on the untilDay", example = "01:00:00")
  val untilTime: LocalTime? = null,
)
