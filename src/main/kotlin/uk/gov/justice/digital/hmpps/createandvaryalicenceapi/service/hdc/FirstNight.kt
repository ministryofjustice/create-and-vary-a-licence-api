package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc

import java.time.LocalTime

data class FirstNight(
  val firstNightFrom: LocalTime,
  val firstNightUntil: LocalTime,
)
