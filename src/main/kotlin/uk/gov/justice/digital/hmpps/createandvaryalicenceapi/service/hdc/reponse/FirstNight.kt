package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse

import java.time.LocalTime

data class FirstNight(
  val firstNightFrom: LocalTime? = null,
  val firstNightUntil: LocalTime? = null,
)
