package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import java.time.LocalDateTime

data class HdcStatusChangedEvent(
  val eventType: String,
  val occurredAt: LocalDateTime,
  val licenceId: Long,
  val bookingId: Long,
  val nomsNumber: String,
  val triggeredBy: String,
  val reason: String? = null,
)
