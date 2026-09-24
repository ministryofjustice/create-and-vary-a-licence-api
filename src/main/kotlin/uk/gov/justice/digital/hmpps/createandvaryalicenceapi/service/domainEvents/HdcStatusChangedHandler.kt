package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

const val HDC_OPT_OUT_EVENT_TYPE = "OPT_OUT"

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
) : EventHandler {
  companion object {
    private val log = LoggerFactory.getLogger(HdcStatusChangedHandler::class.java)
  }

  fun handleOptout(message: String) {
    val event = mapper.readValue(message, HdcStatusChangedEvent::class.java)

    log.info(
      "HDC opt-out processed: eventType={} occurredAt={} licenceId={} bookingId={} nomsNumber={} triggeredBy={} reason={}",
      event.eventType,
      event.occurredAt,
      event.licenceId,
      event.bookingId,
      event.nomsNumber,
      event.triggeredBy,
      event.reason,
    )
  }

  override fun handleEvent(message: String) {
    handleOptout(message)
  }
}
