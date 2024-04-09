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
  private val workingDaysService = mock<WorkingDaysService>()

  private val service = TimeOutLicencesService(
    licenceRepository,
    releaseDateService,
    auditEventRepository,
    licenceEventRepository,
    workingDaysService,
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
      workingDaysService,
    )
  }

  @Test
  fun `return if job execution date is on bank holiday or weekend`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(true)

    service.timeOutLicences()

    verify(releaseDateService, times(0)).getCutOffDateForLicenceTimeOut()
    verify(licenceRepository, times(0)).getAllLicencesToTimeOut()
  }

  @Test
  fun `should not update licences status if there are no eligible licences`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(emptyList())

    service.timeOutLicences()

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())

    verify(auditEventRepository, times(0)).saveAndFlush(any())
    verify(licenceEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `should update licences status if there are eligible licences`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )
    whenever(releaseDateService.isInHardStopPeriod(aLicenceEntity)).thenReturn(
      true,
    )

    service.timeOutLicences()

    val licenceCaptor = argumentCaptor<List<CrdLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

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

  @Test
  fun `should not update licences status if there are ineligible licences`() {
    val anIneligibleLicence = aLicenceEntity.copy(
      id = 2L,
      licenceStartDate = LocalDate.now(clock).plusDays(1),
    )

    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(
      listOf(
        aLicenceEntity,
        anIneligibleLicence,
      ),
    )
    whenever(releaseDateService.isInHardStopPeriod(aLicenceEntity)).thenReturn(
      true,
    )
    whenever(releaseDateService.isInHardStopPeriod(anIneligibleLicence)).thenReturn(
      false,
    )

    service.timeOutLicences()

    val licenceCaptor = argumentCaptor<List<CrdLicence>>()
    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)
    val eventCaptor = ArgumentCaptor.forClass(LicenceEvent::class.java)

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.allValues.size).isEqualTo(1)

    assertThat(licenceCaptor.firstValue[0])
      .extracting("id", "statusCode", "updatedByUsername")
      .isEqualTo(listOf(1L, LicenceStatus.TIMED_OUT, "SYSTEM"))

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
