package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.HdcApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.OutboundEventsPublisher
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent
import java.time.Duration
import java.time.LocalDateTime

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["hdc.event.listener.enabled=true"])
abstract class HdcEventsIntegrationTestBase : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var hdcEventsListener: HdcEventsListener

  @MockitoSpyBean
  lateinit var eventsPublisher: OutboundEventsPublisher

  @BeforeEach
  fun setupClient() {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIdsNoResult()
    hdcApiMockServer.stubGetHdcLicenceData(123L)
  }

  protected fun sendMessage(messageBody: String, eventType: String) {
    hdcCvlEventsSqsClient.sendMessage(
      SendMessageRequest.builder()
        .queueUrl(hdcCvlEventsQueueUrl)
        .messageBody(messageBody)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType).build(),
          ),
        )
        .build(),
    ).get()
  }

  protected fun assertSqsProcessed(timesProcessed: Int = 1) {
    await.atMost(Duration.ofSeconds(30)) untilAsserted {
      verify(hdcEventsListener, times(timesProcessed)).finishedEventProcessing(anyOrNull())
      assertThat(getNumberOfMessagesCurrentlyOnHdcQueue()).isEqualTo(0)
    }
  }

  protected fun createHdcStatusChangedEvent(
    licenceId: Long = 1L,
    bookingId: Long = 12347L,
    nomsNumber: String = "C1234CC",
    reason: String = "Offender opted out",
    triggeredBy: String = "test.user",
    occurredAt: LocalDateTime = LocalDateTime.now(),
  ) = HdcStatusChangedEvent(
    occurredAt = occurredAt,
    licenceId = licenceId,
    bookingId = bookingId,
    nomsNumber = nomsNumber,
    triggeredBy = triggeredBy,
    reason = reason,
  )

  companion object {
    @RegisterExtension
    private val prisonerSearchMockServer = PrisonerSearchMockServer()

    @RegisterExtension
    val hdcApiMockServer = HdcApiMockServer()
  }
}
