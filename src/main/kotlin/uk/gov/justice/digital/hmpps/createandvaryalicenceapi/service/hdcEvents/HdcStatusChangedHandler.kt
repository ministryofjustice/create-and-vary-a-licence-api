package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcLifecycleService

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
  private val hdcLifecycleService: HdcLifecycleService,
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

  fun handleOptOut(message: String) {
    val event = parseEvent(message)
    hdcLifecycleService.convertToCrdLicence(event.nomsNumber, HdcCvlEventType.OPT_OUT)
    logEventProcessed(HdcCvlEventType.OPT_OUT, event)
  }

  fun handleRefuse(message: String) {
    val event = parseEvent(message)
    hdcLifecycleService.convertToCrdLicence(event.nomsNumber, HdcCvlEventType.REFUSE)
    logEventProcessed(HdcCvlEventType.REFUSE, event)
  }

  fun handlePostpone(message: String) {
    val event = parseEvent(message)
    hdcLifecycleService.postponeLicence(event.nomsNumber)
    logEventProcessed(HdcCvlEventType.POSTPONE, event)
  }

  fun handleResume(message: String) {
    val event = parseEvent(message)
    hdcLifecycleService.resumeHdcPostponement(event.nomsNumber)
    logEventProcessed(HdcCvlEventType.RESUME, event)
  }

  fun handleOptIn(message: String) {
    val event = parseEvent(message)
    hdcLifecycleService.convertToHdcLicence(event.nomsNumber)
    logEventProcessed(HdcCvlEventType.OPT_IN, event)
  }
}
