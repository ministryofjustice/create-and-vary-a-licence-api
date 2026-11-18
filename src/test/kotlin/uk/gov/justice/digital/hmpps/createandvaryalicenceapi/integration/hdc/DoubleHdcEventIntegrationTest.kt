package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.hdc

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcCvlEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdcEvents.HdcStatusChangedEvent

class DoubleHdcEventIntegrationTest : HdcEventsIntegrationTestBase() {

  @Sql("classpath:test_data/seed-licence-id-1.sql")
  @Test
  fun `Duplicate OPT_IN creates only one replacement licence`() {
    // Given
    val event = createHdcStatusChangedEvent(bookingId = 123L, nomsNumber = "A1234AA")

    // When
    sendEventTwice(event, HdcCvlEventType.OPT_IN)

    // Then
    assertOneConversion()
  }

  @Sql("classpath:test_data/seed-completed-hdc-licence-1.sql")
  @Test
  fun `Duplicate OPT_OUT creates only one replacement licence`() {
    // Given
    val event = createHdcStatusChangedEvent()

    // When
    sendEventTwice(event, HdcCvlEventType.OPT_OUT)

    // Then
    assertOneConversion()
  }

  @Sql("classpath:test_data/seed-completed-hdc-licence-1.sql")
  @Test
  fun `Duplicate REFUSE creates only one replacement licence`() {
    // Given
    val event = createHdcStatusChangedEvent()

    // When
    sendEventTwice(event, HdcCvlEventType.REFUSE)

    // Then
    assertOneConversion()
  }

  @Sql("classpath:test_data/seed-hdc-approved-and-submitted-licences.sql")
  @Test
  fun `Duplicate POSTPONE does not repeat the postponement side effects`() {
    // Given
    val event = createHdcStatusChangedEvent(licenceId = 2L, bookingId = 12345L, nomsNumber = "A1234AA")

    // When
    sendEventTwice(event, HdcCvlEventType.POSTPONE)

    // Then
    assertThat(testRepository.findAllLicence()).hasSize(2)
    assertThat(testRepository.findAllHdcPostponements()).hasSize(2)
    assertThat(testRepository.findAllAuditEvents()).hasSize(4)
    verify(eventsPublisher).publishDomainEvent(anyOrNull())
  }

  @Sql(
    "classpath:test_data/seed-hdc-licence-id-1.sql",
    "classpath:test_data/seed-hdc-postponement-for-licence-id-1.sql",
  )
  @Test
  fun `Duplicate RESUME audits the change only once`() {
    // Given
    val event = createHdcStatusChangedEvent(bookingId = 54321L, nomsNumber = "A1234AA")

    // When
    sendEventTwice(event, HdcCvlEventType.RESUME)

    // Then
    assertThat(testRepository.findAllHdcPostponements()).hasSize(1)
    assertThat(testRepository.findAllAuditEvents()).hasSize(1)
    verifyNoInteractions(eventsPublisher)
  }

  @Sql("classpath:test_data/seed-hdc-licence-id-1.sql")
  @Test
  fun `Duplicate ADDRESS_WITHDRAWN has no persisted side effects`() {
    // Given
    val event = createHdcStatusChangedEvent(bookingId = 54321L, nomsNumber = "A1234AA")

    // When
    sendEventTwice(event, HdcCvlEventType.ADDRESS_WITHDRAWN)

    // Then
    assertNoSideEffects()
  }

  @Sql("classpath:test_data/seed-hdc-licence-id-1.sql")
  @Test
  fun `Duplicate RISK_CHECKS_SUBMITTED has no persisted side effects`() {
    // Given
    val event = createHdcStatusChangedEvent(bookingId = 54321L, nomsNumber = "A1234AA")

    // When
    sendEventTwice(event, HdcCvlEventType.RISK_CHECKS_SUBMITTED)

    // Then
    assertNoSideEffects()
  }

  private fun sendEventTwice(event: HdcStatusChangedEvent, eventType: HdcCvlEventType) {
    val message = mapper.writeValueAsString(event)
    sendMessage(message, eventType.name)
    assertSqsProcessed()
    sendMessage(message, eventType.name)
    assertSqsProcessed(timesProcessed = 2)
  }

  private fun assertOneConversion() {
    assertThat(testRepository.findAllLicence()).hasSize(2)
    assertThat(testRepository.findAllLicenceLinks()).hasSize(1)
    assertThat(testRepository.findAllAuditEvents()).hasSize(4)
    verify(eventsPublisher).publishDomainEvent(anyOrNull())
  }

  private fun assertNoSideEffects() {
    assertThat(testRepository.findAllLicence()).hasSize(2)
    assertThat(testRepository.findAllLicenceLinks()).isEmpty()
    assertThat(testRepository.findAllHdcPostponements()).isEmpty()
    assertThat(testRepository.findAllAuditEvents()).isEmpty()
    verifyNoInteractions(eventsPublisher)
  }
}
