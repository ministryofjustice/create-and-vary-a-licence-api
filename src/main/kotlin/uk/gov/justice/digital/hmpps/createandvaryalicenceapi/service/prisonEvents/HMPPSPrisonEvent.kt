package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents

import java.time.LocalDateTime

open class HMPPSPrisonEvent(
  val eventDatetime: LocalDateTime,
  val offenderIdDisplay: String?,
)

class SentenceDatesChangedEvent(
  eventDatetime: LocalDateTime,
  offenderIdDisplay: String? = null,
  val bookingId: Long,
  val sentenceCalculationId: Long,
) : HMPPSPrisonEvent(
  eventDatetime = eventDatetime,
  offenderIdDisplay = offenderIdDisplay,
)
