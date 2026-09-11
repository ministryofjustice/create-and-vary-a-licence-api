package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.prisonEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.UpdateSentenceDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.PrisonEventsListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SENTENCE_DATES_CHANGED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SentenceDatesChangedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prisonEvents.SentenceDatesChangedHandler
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

const val BOOKING_ID = 4576L

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

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `A sentence dates changed event is processed`() {
    prisonApiMockServer.stubGetCourtOutcomes()
    prisonApiMockServer.stubGetPrisonerDetail()
    prisonerSearchMockServer.stubSearchPrisonersByBookingIds()

    val event = buildSentenceDatesChangedEventJson()
    val message = mapper.writeValueAsString(event)

    sendEvent(message)

    verify(sentenceDatesChangedHandler).handleEvent(message)
    verify(updateSentenceDateService).updateSentenceDates(1L)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-active-hdc-licence-id-1.sql",
  )
  fun `A sentence dates changed event updates the CRD for an active HDC licence and its in-progress variation`() {
    val newCrd = LocalDate.now().plusDays(30)
    prisonApiMockServer.stubGetSentencesAndOffences(54321)
    prisonApiMockServer.stubGetPrisonerDetail(
      nomsId = "A1234AA",
      sentenceDetail = SentenceDetail(conditionalReleaseDate = newCrd),
    )
    prisonerSearchMockServer.stubSearchPrisonersByBookingIds()

    val event = buildSentenceDatesChangedEventJson()
    val message = mapper.writeValueAsString(event)

    sendEvent(message)

    verify(sentenceDatesChangedHandler).handleEvent(message)
    verify(updateSentenceDateService, never()).updateSentenceDates(any())
    val activeLicence = testRepository.findLicence(1)
    val variationLicence = testRepository.findLicence(2)
    assertThat(activeLicence.conditionalReleaseDate).isEqualTo(newCrd)
    assertThat(variationLicence.conditionalReleaseDate).isEqualTo(newCrd)
  }

  @Test
  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
  )
  fun `A sentence dates changed event updates the CRD for a pre-release HDC licence`() {
    val newCrd = LocalDate.now().plusDays(30)
    prisonApiMockServer.stubGetPrisonerDetail(
      nomsId = "A1234AA",
      sentenceDetail = SentenceDetail(conditionalReleaseDate = newCrd),
    )
    prisonerSearchMockServer.stubSearchPrisonersByBookingIds()

    val event = buildSentenceDatesChangedEventJson()
    val message = mapper.writeValueAsString(event)

    sendEvent(message)

    verify(sentenceDatesChangedHandler).handleEvent(message)
    verify(updateSentenceDateService, never()).updateSentenceDates(any())
    val licence = testRepository.findLicence(1)
    assertThat(licence.conditionalReleaseDate).isEqualTo(newCrd)
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

  fun buildSentenceDatesChangedEventJson() = SentenceDatesChangedEvent(
    eventDatetime = LocalDateTime.of(2026, Month.JANUARY, 11, 6, 32, 53),
    bookingId = BOOKING_ID,
    sentenceCalculationId = 3L,
  )

  private companion object {
    @RegisterExtension
    val prisonApiMockServer = PrisonApiMockServer()

    @RegisterExtension
    val prisonerSearchMockServer = PrisonerSearchMockServer()
  }
}
