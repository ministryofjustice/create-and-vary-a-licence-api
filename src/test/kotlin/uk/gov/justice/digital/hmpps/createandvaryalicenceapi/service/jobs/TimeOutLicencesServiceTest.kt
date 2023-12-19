package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.LicenceEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.TimeOutLicencesService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AuditEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceEventType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimeOutLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()

  private val service = TimeOutLicencesService(
    licenceRepository,
    releaseDateService,
    auditEventRepository,
    licenceEventRepository,
    clock,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      releaseDateService,
      auditEventRepository,
      licenceEventRepository,
    )
  }

  @Test
  fun `return if job execution date is on bank holiday or weekend`() {
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now(clock))).thenReturn(true)

    service.timeOutLicencesJob()

    verify(releaseDateService, times(0)).getCutOffDateForLicenceTimeOut(any())
    verify(licenceRepository, times(0)).getAllLicencesToTimeOut(any())
  }

  @Test
  fun `should not update licences status if there are no eligible licences`() {
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now(clock))).thenReturn(false)
    whenever(releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now(clock))).thenReturn(LocalDate.parse("2023-12-07"))
    whenever(licenceRepository.getAllLicencesToTimeOut(LocalDate.now(clock))).thenReturn(emptyList())

    service.timeOutLicencesJob()

    verify(releaseDateService, times(1)).getCutOffDateForLicenceTimeOut(LocalDate.parse("2023-12-05"))
    verify(licenceRepository, times(1)).getAllLicencesToTimeOut(LocalDate.parse("2023-12-07"))

    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())

    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `should update licences status if there are eligible licences`() {
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now(clock))).thenReturn(false)
    whenever(releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now(clock))).thenReturn(
      LocalDate.now(clock).plusDays(2),
    )
    whenever(licenceRepository.getAllLicencesToTimeOut(LocalDate.now(clock).plusDays(2))).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )

    service.timeOutLicencesJob()

    val licenceCaptor = argumentCaptor<List<CrdLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(releaseDateService, times(1)).getCutOffDateForLicenceTimeOut(LocalDate.parse("2023-12-05"))
    verify(licenceRepository, times(1)).getAllLicencesToTimeOut(LocalDate.parse("2023-12-07"))

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.firstValue[0])
      .extracting("statusCode", "updatedByUsername")
      .isEqualTo(listOf(LicenceStatus.TIMED_OUT, "SYSTEM"))

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(licenceEventRepository, times(1)).saveAndFlush(eventCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "eventType", "summary", "detail")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          AuditEventType.SYSTEM_EVENT,
          "Licence automatically timed out for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
          "ID ${aLicenceEntity.id} type ${aLicenceEntity.typeCode} status ${LicenceStatus.TIMED_OUT} version ${aLicenceEntity.version}",
        ),
      )

    assertThat(eventCaptor.value)
      .extracting("licenceId", "eventType", "username", "forenames", "surname", "eventDescription")
      .isEqualTo(
        listOf(
          1L,
          LicenceEventType.TIMED_OUT,
          "SYSTEM",
          "SYSTEM",
          "SYSTEM",
          "Licence automatically timed out for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())
    val aLicenceEntity = TestData.createCrdLicence().copy()
  }
}
