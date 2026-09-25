package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import com.fasterxml.jackson.annotation.JsonProperty

data class HdcMessage(
  @field:JsonProperty("Message") val message: String,
  @field:JsonProperty("MessageAttributes") val messageAttributes: HdcMessageAttributes,
)

data class HdcMessageAttributes(
  @field:JsonProperty("eventType") val eventType: HdcEventTypeAttribute,
)

data class HdcEventTypeAttribute(
  @field:JsonProperty("Value") val value: String,
  @field:JsonProperty("Type") val type: String,
)
