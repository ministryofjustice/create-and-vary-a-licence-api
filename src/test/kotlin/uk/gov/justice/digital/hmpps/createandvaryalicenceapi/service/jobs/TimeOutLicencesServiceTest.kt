package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.workingDays.WorkingDaysService
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TimeOutLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val workingDaysService = mock<WorkingDaysService>()
  private val licenceService = mock<LicenceService>()

  private val service = TimeOutLicencesService(
    licenceRepository,
    releaseDateService,
    workingDaysService,
    clock,
    licenceService,
  )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      releaseDateService,
      workingDaysService,
      licenceService,
    )
  }

  @Test
  fun `return if job execution date is on bank holiday or weekend`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(true)

    service.timeOutLicences()

    verify(licenceRepository, times(0)).getAllLicencesToTimeOut()
  }

  @Test
  fun `should not update licences status if there are no eligible licences`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(emptyList())

    service.timeOutLicences()

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())
  }

  @Test
  fun `should update licences status if there are eligible licences`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(
      listOf(
        aLicenceEntity,
      ),
    )
    whenever(releaseDateService.isInHardStopPeriod(aLicenceEntity.licenceStartDate, aLicenceEntity.kind, clock)).thenReturn(
      true,
    )

    service.timeOutLicences()

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verify(licenceService, times(1)).timeout(aLicenceEntity, "due to reaching hard stop")
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
    whenever(releaseDateService.isInHardStopPeriod(aLicenceEntity.licenceStartDate, aLicenceEntity.kind, clock)).thenReturn(
      true,
    )
    whenever(releaseDateService.isInHardStopPeriod(anIneligibleLicence.licenceStartDate, anIneligibleLicence.kind, clock)).thenReturn(
      false,
    )

    service.timeOutLicences()

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verify(licenceService, times(1)).timeout(aLicenceEntity, "due to reaching hard stop")
    verify(licenceService, times(0)).timeout(anIneligibleLicence, "due to reaching hard stop")
  }

  @Test
  fun `should not time out time served licences`() {
    whenever(workingDaysService.isNonWorkingDay(LocalDate.now(clock))).thenReturn(false)
    whenever(licenceRepository.getAllLicencesToTimeOut()).thenReturn(
      listOf(
        aTimeServedLicence,
      ),
    )
    whenever(releaseDateService.isInHardStopPeriod(aTimeServedLicence.licenceStartDate, aTimeServedLicence.kind, clock)).thenReturn(
      false,
    )

    service.timeOutLicences()

    verify(licenceRepository, times(1)).getAllLicencesToTimeOut()

    verifyNoInteractions(licenceService)
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-12-05T00:00:00Z"), ZoneId.systemDefault())
    val aLicenceEntity = createCrdLicence()
    val aTimeServedLicence = createTimeServedLicence()
  }
}
