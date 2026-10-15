package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
  private val hdcService: HdcService,
) {

  private val log = LoggerFactory.getLogger(HdcStatusChangedHandler::class.java)

  private fun parseEvent(message: String) = mapper.readValue(message, HdcStatusChangedEvent::class.java)

  private fun logEventProcessed(eventType: HdcCvlEventType, event: HdcStatusChangedEvent) {
    log.info(
      "HDC {} processed: occurredAt={} licenceId={} bookingId={} nomsNumber={} triggeredBy={} reason={}",
      eventType.name.lowercase(),
      event.occurredAt,
      event.licenceId,
      event.bookingId,
      event.nomsNumber,
      event.triggeredBy,
      event.reason,
    )
  }

  fun handleOptout(message: String) {
    val event = parseEvent(message)
    hdcService.convertToCrdLicence(event.nomsNumber, HdcCvlEventType.OPT_OUT)
    logEventProcessed(HdcCvlEventType.OPT_OUT, event)
  }

  fun handleRefuse(message: String) {
    val event = parseEvent(message)
    hdcService.convertToCrdLicence(event.nomsNumber, HdcCvlEventType.REFUSE)
    logEventProcessed(HdcCvlEventType.REFUSE, event)
  }

  fun handlePostpone(message: String) {
    val event = parseEvent(message)
    hdcService.postponeLicence(event.nomsNumber)
    logEventProcessed(HdcCvlEventType.POSTPONE, event)
  }

  fun handleResume(message: String) {
    val event = parseEvent(message)
    hdcService.resumeHdcPostponement(event.nomsNumber)
    logEventProcessed(HdcCvlEventType.RESUME, event)
  }
}
