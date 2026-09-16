package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.prisonEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneId

const val BOOKING_ID = 4576L
private val FIXED_INSTANT = Instant.parse("2024-04-22T00:00:00Z")
private val FIXED_ZONE = ZoneId.of("UTC")

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["domain.event.listener.disabled=false", "prison.event.listener.enabled=true"])
@Import(PrisonEventsListenerIntegrationTest.FixedClockTestConfiguration::class)
class PrisonEventsListenerIntegrationTest : IntegrationTestBase() {
  private val fixedClock = Clock.fixed(FIXED_INSTANT, FIXED_ZONE)

  @TestConfiguration
  class FixedClockTestConfiguration {
    @Bean
    @Primary
    fun clock(): Clock = Clock.fixed(FIXED_INSTANT, FIXED_ZONE)
  }

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
    val newCrd = LocalDate.now(fixedClock).plusDays(30)
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

    val auditEvent = testRepository.findFirstAuditEvent(1)
    assertThat(auditEvent.summary).contains("Updated HDC conditional release date")
    assertThat(auditEvent.changes).containsEntry("type", "Updated HDC conditional release date")
      .containsKey("before")
      .containsKey("after")
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
