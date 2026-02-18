package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.prisonEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.PrisonEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SENTENCE_DATES_CHANGED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SentenceDatesChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SentenceDatesChangedHandler
import java.time.Duration
import java.time.LocalDateTime
import java.time.Month

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["domain.event.listener.disabled=false", "prison.event.listener.enabled=true"])
class PrisonEventsListenerIntegrationTest : IntegrationTestBase() {
  @MockitoSpyBean
  lateinit var sentenceDatesChangedHandler: SentenceDatesChangedHandler

  @MockitoSpyBean
  lateinit var updateSentenceDateService: UpdateSentenceDateService

  @MockitoSpyBean
  lateinit var prisonEventsListener: PrisonEventsListener

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @BeforeEach
  fun setupClient() {
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `A sentence dates changed event is processed`() {
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonApiMockServer.stubGetPrisonerDetail()
    prisonerSearchMockServer.stubSearchPrisonersByBookingIds(nomisId = "A1234AA")

    val event = buildSentenceDatesChangedEventJson(bookingId = 4576)
    val message = mapper.writeValueAsString(event)

    sendEvent(message)

    verify(sentenceDatesChangedHandler).handleEvent(message)
    verify(updateSentenceDateService).updateSentenceDates(1L)
  }

  private fun sendEvent(message: String) {
    prisonEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(prisonEventsTopicArn)
        .message(message)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue(SENTENCE_DATES_CHANGED_EVENT_TYPE).build(),
          ),
        )
        .build(),
    )

    awaitAtMost30Secs untilAsserted {
      verify(prisonEventsListener).finishedEventProcessing(any())
    }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  fun buildSentenceDatesChangedEventJson(
    bookingId: Long,
  ) = SentenceDatesChangedEvent(
    eventDatetime = LocalDateTime.of(2026, Month.JANUARY, 11, 6, 32, 53),
    bookingId = bookingId,
    sentenceCalculationId = 3L,
  )

  private companion object {
    val govUkApiMockServer = GovUkMockServer()
    val prisonApiMockServer = PrisonApiMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
      prisonApiMockServer.start()
      prisonerSearchMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
      prisonApiMockServer.stop()
      prisonerSearchMockServer.stop()
    }
  }
}
