package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
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
      hdcCreationEnabled = true,
    )
  }

  @Test
  fun `handleOptout processes event when enabled`() {
    handler.handleOptout(eventJson)

    verify(hdcService).convertToCrdLicence("A1234BC")
  }

  @Test
  fun `handleOptout does not process when disabled`() {
    handler = HdcStatusChangedHandler(
      mapper = mapper,
      hdcService = hdcService,
      hdcCreationEnabled = false,
    )

    handler.handleOptout(eventJson)

    verifyNoInteractions(hdcService)
  }

  @Test
  fun `handlePostpone processes event when enabled`() {
    handler.handlePostpone(eventJson)

    verify(hdcService).transitionHdcLicenceToInProgress("A1234BC")
  }

  @Test
  fun `handlePostpone does not process when disabled`() {
    handler = HdcStatusChangedHandler(
      mapper = mapper,
      hdcService = hdcService,
      hdcCreationEnabled = false,
    )

    handler.handlePostpone(eventJson)

    verifyNoInteractions(hdcService)
  }

  @Test
  fun `handleOptout calls service before logging`() {
    handler.handleOptout(eventJson)

    val inOrderVerifier = inOrder(hdcService)
    inOrderVerifier.verify(hdcService).convertToCrdLicence("A1234BC")
  }

  @Test
  fun `handlePostpone calls service before logging`() {
    handler.handlePostpone(eventJson)

    val inOrderVerifier = inOrder(hdcService)
    inOrderVerifier.verify(hdcService).transitionHdcLicenceToInProgress("A1234BC")
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
