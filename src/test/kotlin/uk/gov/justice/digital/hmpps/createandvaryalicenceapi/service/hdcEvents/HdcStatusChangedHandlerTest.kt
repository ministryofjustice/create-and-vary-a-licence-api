package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import tools.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcLifecycleService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcStatusService
import java.time.LocalDateTime

class HdcStatusChangedHandlerTest {
  private val mapper = ObjectMapper()
  private val hdcLifecycleService = mock<HdcLifecycleService>()
  private val hdcStatusService = mock<HdcStatusService>()

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
      hdcLifecycleService = hdcLifecycleService,
      hdcStatusService = hdcStatusService,
    )
  }

  @Test
  fun `handleOptout processes event when enabled`() {
    handler.handleOptOut(eventJson)

    verify(hdcStatusService).convertToCrdLicence("A1234BC", HdcCvlEventType.OPT_OUT)
  }

  @Test
  fun `handlePostpone processes event when enabled`() {
    handler.handlePostpone(eventJson)

    verify(hdcStatusService).postponeLicence("A1234BC")
  }

  @Test
  fun `handleOptout calls service before logging`() {
    handler.handleOptOut(eventJson)

    val inOrderVerifier = inOrder(hdcStatusService)
    inOrderVerifier.verify(hdcStatusService).convertToCrdLicence("A1234BC", HdcCvlEventType.OPT_OUT)
  }

  @Test
  fun `handleOptIn processes event`() {
    handler.handleOptIn(eventJson)

    verify(hdcStatusService).convertToHdcLicence("A1234BC")
  }

  @Test
  fun `handleOptIn calls service before logging`() {
    handler.handleOptIn(eventJson)

    val inOrderVerifier = inOrder(hdcStatusService)
    inOrderVerifier.verify(hdcStatusService).convertToHdcLicence("A1234BC")
  }

  @Test
  fun `handleRefused processes event`() {
    handler.handleRefuse(eventJson)

    verify(hdcStatusService).convertToCrdLicence("A1234BC", HdcCvlEventType.REFUSE)
  }

  @Test
  fun `handlePostpone calls service before logging`() {
    handler.handlePostpone(eventJson)

    val inOrderVerifier = inOrder(hdcStatusService)
    inOrderVerifier.verify(hdcStatusService).postponeLicence("A1234BC")
  }

  @Test
  fun `handleResume processes event`() {
    handler.handleResume(eventJson)

    verify(hdcStatusService).resumeHdcPostponement("A1234BC")
  }

  @Test
  fun `handleAddressWithdrawn processes event without changing the licence`() {
    handler.handleAddressWithdrawn(eventJson)

    verifyNoInteractions(hdcLifecycleService, hdcStatusService)
  }

  @Test
  fun `handleRiskChecksSubmitted refreshes the HDC curfew address`() {
    handler.handleRiskChecksSubmitted(eventJson)

    verify(hdcLifecycleService).refreshHdcCurfewAddresses("A1234BC")
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
