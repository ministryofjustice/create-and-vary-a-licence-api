package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.EventQueryObject
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import java.time.LocalDateTime

class EventServiceTest {
  private val licenceEventRepository = mock<LicenceEventRepository>()

  private val service = EventService(licenceEventRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(licenceEventRepository)
  }

  @Test
  fun `find events matching criteria - no parameters matches all`() {
    val eventQueryObject = EventQueryObject()
    whenever(
      licenceEventRepository.findAll(any<Specification<LicenceEvent>>(), any<Sort>()),
    )
      .thenReturn(
        listOf(aLicenceEventEntity),
      )

    val events = service.findEventsMatchingCriteria(eventQueryObject)

    assertThat(events).isEqualTo(listOf(transform(aLicenceEventEntity)))
    verify(licenceEventRepository, times(1)).findAll(
      any<Specification<LicenceEvent>>(),
      ArgumentMatchers.eq(Sort.unsorted()),
    )
  }

  @Test
  fun `find events matching criteria - multiple parameters`() {
    val eventQueryObject = EventQueryObject(
      licenceId = 1,
      eventTypes = listOf(LicenceEventType.VARIATION_SUBMITTED),
    )
    whenever(
      licenceEventRepository.findAll(any<Specification<LicenceEvent>>(), any<Sort>()),
    )
      .thenReturn(
        listOf(aLicenceEventEntity),
      )

    val events = service.findEventsMatchingCriteria(eventQueryObject)

    assertThat(events).isEqualTo(listOf(transform(aLicenceEventEntity)))
    verify(licenceEventRepository, times(1)).findAll(
      any<Specification<LicenceEvent>>(),
      ArgumentMatchers.eq(Sort.unsorted()),
    )
  }

  private companion object {
    val aLicenceEventEntity = LicenceEvent(
      licenceId = 1,
      eventType = LicenceEventType.SUBMITTED,
      username = "tcom",
      forenames = "Test",
      surname = "Com",
      eventDescription = "Licence submitted for approval",
      eventTime = LocalDateTime.now(),
    )
  }
}
