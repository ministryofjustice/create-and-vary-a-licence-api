package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import jakarta.transaction.Transactional
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

  @Transactional
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

    hdcService.convertToCrdLicence(event.nomsNumber)
  }
}
