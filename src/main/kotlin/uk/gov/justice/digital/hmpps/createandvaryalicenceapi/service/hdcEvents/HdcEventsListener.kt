package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import io.awspring.cloud.sqs.annotation.SqsListener
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.messaging.Message
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.ADDRESS_WITHDRAWN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_IN
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.OPT_OUT
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.POSTPONE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.REFUSE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.RESUME
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.RISK_CHECKS_SUBMITTED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.ELIGIBLE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType.INELIGIBLE

@ConditionalOnProperty(name = ["hdc.event.listener.enabled"], havingValue = "true", matchIfMissing = true)
@Service
class HdcEventsListener(
  private val hdcEventHandler: HdcEventHandler,
  private val mapper: ObjectMapper,
) {
  private val log = LoggerFactory.getLogger(HdcEventsListener::class.java)

  @SqsListener(
    "hdccvleventsqueue",
    factory = "hmppsQueueContainerFactoryProxy",
    pollTimeoutSeconds = "\${hmpps.sqs.poll-timeout-seconds:}",
  )
  fun onMessage(message: Message<String>) {
    val rawMessage = message.payload
    log.debug("Raw HDC event message received: {}", rawMessage)

    var eventType: HdcCvlEventType? = null
    try {
      val event = mapper.readValue(rawMessage, HdcStatusChangedEvent::class.java)
      eventType = getHdcEventType(message)
      log.debug("Successfully parsed HDC event | eventType={} | licenceId={}", eventType, event.licenceId)
      when (eventType) {
        OPT_OUT -> hdcEventHandler.handleOptOut(rawMessage)
        REFUSE -> hdcEventHandler.handleRefuse(rawMessage)
        POSTPONE -> hdcEventHandler.handlePostpone(rawMessage)
        RESUME -> hdcEventHandler.handleResume(rawMessage)
        OPT_IN -> hdcEventHandler.handleOptIn(rawMessage)
        ADDRESS_WITHDRAWN -> hdcEventHandler.handleAddressWithdrawn(rawMessage)
        RISK_CHECKS_SUBMITTED -> hdcEventHandler.handleRiskChecksSubmitted(rawMessage)
        ELIGIBLE -> hdcEventHandler.handleEligible(rawMessage)
        INELIGIBLE -> hdcEventHandler.handleIneligible(rawMessage)
      }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
      log.error("Failed to parse HDC message - likely format mismatch. Raw message: {}", rawMessage, e)
      throw e
    } finally {
      finishedEventProcessing(eventType)
    }
  }

  private fun getHdcEventType(
    message: Message<String>,
  ): HdcCvlEventType {
    val eventTypeValue = requireNotNull(message.headers["eventType"] as? String) {
      "Missing eventType in message attributes"
    }
    return HdcCvlEventType.valueOf(eventTypeValue)
  }

  fun finishedEventProcessing(eventType: HdcCvlEventType?) {
    log.info("Processed HDC event: {}", eventType)
  }
}
