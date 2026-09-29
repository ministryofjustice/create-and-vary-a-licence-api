package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService

@Service
class HdcStatusChangedHandler(
  private val mapper: ObjectMapper,
  private val hdcService: HdcService,
) {
  companion object {
    private val log = LoggerFactory.getLogger(HdcStatusChangedHandler::class.java)
  }

  @Transactional
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

    hdcService.convertToCrdLicence(event.nomsNumber)
  }
}
