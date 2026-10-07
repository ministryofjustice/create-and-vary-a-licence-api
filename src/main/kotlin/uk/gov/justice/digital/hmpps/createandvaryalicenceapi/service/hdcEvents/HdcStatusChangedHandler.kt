package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
  private val hdcService: HdcService,
  @param:Value("\${feature.toggle.hdcCreation.enabled}") private val hdcCreationEnabled: Boolean = false,
) {

  private val log = LoggerFactory.getLogger(HdcStatusChangedHandler::class.java)

  private fun processIfEnabled(eventType: HdcCvlEventType, block: () -> Unit) {
    if (!hdcCreationEnabled) {
      log.info("HDC {} processing disabled", eventType.name.lowercase())
      return
    }
    block()
  }

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
    processIfEnabled(HdcCvlEventType.OPT_OUT) {
      val event = parseEvent(message)
      hdcService.convertToCrdLicence(event.nomsNumber)
      logEventProcessed(HdcCvlEventType.OPT_OUT, event)
    }
  }

  fun handlePostpone(message: String) {
    processIfEnabled(HdcCvlEventType.POSTPONE) {
      val event = parseEvent(message)
      hdcService.transitionHdcLicenceToInProgress(event.nomsNumber)
      logEventProcessed(HdcCvlEventType.POSTPONE, event)
    }
  }
}
