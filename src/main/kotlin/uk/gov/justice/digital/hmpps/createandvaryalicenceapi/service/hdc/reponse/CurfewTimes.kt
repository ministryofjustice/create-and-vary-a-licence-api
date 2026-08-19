package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse

import java.time.DayOfWeek
import java.time.LocalTime

data class CurfewTimes(
  val fromDay: DayOfWeek,
  val fromTime: LocalTime? = null,
  val untilDay: DayOfWeek,
  val untilTime: LocalTime? = null,
)
