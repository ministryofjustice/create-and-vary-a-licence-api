package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.annotation.JsonProperty

data class HMPPSDomainEvent(
  val eventType: String,
  val additionalInformation: Map<String, Any?> = emptyMap(),
  val detailUrl: String? = null,
  val version: Int,
  val occurredAt: String,
  val description: String? = null,
  val personReference: PersonReference = PersonReference(),
)

data class Message(
  @field:JsonProperty("Message") val message: String,
  @field:JsonProperty("MessageId") val messageId: String,
  @field:JsonProperty("MessageAttributes") val messageAttributes: MessageAttributes,
)

data class MessageAttributes(val eventType: EventType)
data class EventType(
  @field:JsonProperty("Value") val value: String,
  @field:JsonProperty("Type") val type: String,
)

data class PersonReference(val identifiers: List<Identifiers> = listOf()) {
  fun crn() = identifiers.find { it.type == "CRN" }?.value!!
  fun noms() = identifiers.find { it.type == "NOMS" }?.value
}

data class Identifiers(val type: String, val value: String? = null)
