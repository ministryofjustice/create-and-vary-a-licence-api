package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
) {
  companion object {
    private val log = LoggerFactory.getLogger(HdcStatusChangedHandler::class.java)
  }

  fun handleOptout(message: String) {
    val event = mapper.readValue(message, HdcStatusChangedEvent::class.java)

    log.info(
      "HDC opt-out processed: occurredAt={} licenceId={} bookingId={} nomsNumber={} triggeredBy={} reason={}",
      event.occurredAt,
      event.licenceId,
      event.bookingId,
      event.nomsNumber,
      event.triggeredBy,
      event.reason,
    )
  }
}
