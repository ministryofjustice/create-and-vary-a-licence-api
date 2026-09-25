package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import com.fasterxml.jackson.annotation.JsonProperty

data class HdcMessage(
  @field:JsonProperty("message") val message: String,
  @field:JsonProperty("eventType") val eventType: String,
)
