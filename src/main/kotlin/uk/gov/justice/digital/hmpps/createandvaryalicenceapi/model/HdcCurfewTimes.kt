package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model

import io.swagger.v3.oas.annotations.media.Schema
import java.time.DayOfWeek
import java.time.LocalTime

@Schema(description = "Describes the curfew times on this hdc licence")
data class HdcCurfewTimes(

  @Schema(description = "The internal ID for these curfew times on this hdc licence", example = "98987")
  val id: Long? = null,

  @Schema(description = "Sequence of this curfew time within the curfew times", example = "1")
  val curfewTimesSequence: Int? = null,

  @Schema(description = "The day on which this curfew starts for this curfew time", example = "MONDAY")
  val fromDay: DayOfWeek? = null,

  @Schema(description = "The time at which this curfew starts on the fromDay", example = "01:00:00")
  val fromTime: LocalTime? = null,

  @Schema(description = "The day on which this curfew ends for this curfew time", example = "MONDAY")
  val untilDay: DayOfWeek? = null,

  @Schema(description = "The time at which this curfew ends on the untilDay", example = "01:00:00")
  val untilTime: LocalTime? = null,
)
