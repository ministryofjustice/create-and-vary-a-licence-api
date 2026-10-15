package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import java.time.LocalDateTime

class HdcStatusChangedHandlerTest {
  private val mapper = ObjectMapper()
  private val hdcService = mock<HdcService>()

  private lateinit var handler: HdcStatusChangedHandler

  private val eventJson = mapper.writeValueAsString(
    HdcStatusChangedEvent(
      occurredAt = LocalDateTime.of(2024, 1, 15, 10, 30),
      licenceId = 123L,
      bookingId = 456L,
      nomsNumber = "A1234BC",
      triggeredBy = "test.user",
      reason = "Test reason",
    ),
  )

  @BeforeEach
  fun setUp() {
    handler = HdcStatusChangedHandler(
      mapper = mapper,
      hdcService = hdcService,
    )
  }

  @Test
  fun `handleOptout processes event when enabled`() {
    handler.handleOptout(eventJson)

    verify(hdcService).convertToCrdLicence("A1234BC", HdcCvlEventType.OPT_OUT)
  }

  @Test
  fun `handlePostpone processes event when enabled`() {
    handler.handlePostpone(eventJson)

    verify(hdcService).postponeLicence("A1234BC")
  }

  @Test
  fun `handleOptout calls service before logging`() {
    handler.handleOptout(eventJson)

    val inOrderVerifier = inOrder(hdcService)
    inOrderVerifier.verify(hdcService).convertToCrdLicence("A1234BC", HdcCvlEventType.OPT_OUT)
  }

  @Test
  fun `handleRefused processes event`() {
    handler.handleRefused(eventJson)

    verify(hdcService).convertToCrdLicence("A1234BC", HdcCvlEventType.REFUSE)
  }

  @Test
  fun `handlePostpone calls service before logging`() {
    handler.handlePostpone(eventJson)

    val inOrderVerifier = inOrder(hdcService)
    inOrderVerifier.verify(hdcService).postponeLicence("A1234BC")
  }

  @Test
  fun `handleResume processes event`() {
    handler.handleResume(eventJson)

    verify(hdcService).resumeHdcPostponement("A1234BC")
  }

  @Test
  fun `parses event correctly from JSON`() {
    val event = mapper.readValue(eventJson, HdcStatusChangedEvent::class.java)

    assertThat(event.licenceId).isEqualTo(123L)
    assertThat(event.bookingId).isEqualTo(456L)
    assertThat(event.nomsNumber).isEqualTo("A1234BC")
    assertThat(event.triggeredBy).isEqualTo("test.user")
    assertThat(event.reason).isEqualTo("Test reason")
  }
}
