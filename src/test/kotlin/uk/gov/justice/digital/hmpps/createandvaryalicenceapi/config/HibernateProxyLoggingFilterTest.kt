package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.spi.FilterReply
import org.hibernate.engine.internal.StatefulPersistenceContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.PrisonUser

private const val MESSAGE = "HHH000179: Narrowing proxy to class"
private val STATE_PACKAGE = StatefulPersistenceContext::class.java.name

class HibernateProxyLoggingFilterTest {

  private val filter = HibernateProxyLoggingFilterConfig.HibernateProxyLoggingFilter()

  @Test
  fun `should suppressed CommunityOffenderManager warning message`() {
    // Given
    val event = mock<ILoggingEvent>()
    whenever(event.level).thenReturn(Level.WARN)
    whenever(event.loggerName).thenReturn(STATE_PACKAGE)
    whenever(event.formattedMessage).thenReturn(
      "$MESSAGE ${CommunityOffenderManager::class.java.name} - this operation breaks ==",
    )

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.DENY, result)
  }

  @Test
  fun `should suppressed PrisonUser warning message`() {
    // Given
    val event = mock<ILoggingEvent>()
    whenever(event.level).thenReturn(Level.WARN)
    whenever(event.loggerName).thenReturn(STATE_PACKAGE)
    whenever(event.formattedMessage).thenReturn(
      "$MESSAGE ${PrisonUser::class.java.name} - this operation breaks ==",
    )

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.DENY, result)
  }

  @Test
  fun `should allow warning message for non-suppressed class`() {
    // Given
    val event = mock<ILoggingEvent>()
    whenever(event.level).thenReturn(Level.WARN)
    whenever(event.loggerName).thenReturn(STATE_PACKAGE)
    whenever(event.formattedMessage).thenReturn(
      "$MESSAGE some.other.ClassName - this operation breaks ==",
    )

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.NEUTRAL, result)
  }

  @Test
  fun `should allow different logger name`() {
    // Given
    val event = mock<ILoggingEvent>()
    whenever(event.level).thenReturn(Level.WARN)
    whenever(event.loggerName).thenReturn("com.example.other")
    whenever(event.formattedMessage).thenReturn(
      "$MESSAGE ${PrisonUser::class.java.name} - this operation breaks ==",
    )

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.NEUTRAL, result)
  }

  @Test
  fun `should allow non-warning level`() {
    // Given
    val event = mock<ILoggingEvent>()
    whenever(event.level).thenReturn(Level.INFO)
    whenever(event.loggerName).thenReturn(STATE_PACKAGE)
    whenever(event.formattedMessage).thenReturn(
      "$MESSAGE ${PrisonUser::class.java.name} - this operation breaks ==",
    )

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.NEUTRAL, result)
  }

  @Test
  fun `should allow null event`() {
    // Given
    val event: ILoggingEvent? = null

    // When
    val result = filter.decide(event)

    // Then
    assertEquals(FilterReply.NEUTRAL, result)
  }
}
