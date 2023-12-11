package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.TimeOutLicencesService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition

class TimeOutLicencesServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val licenceEventRepository = mock<LicenceEventRepository>()

  private val service = Mockito.spy(
    TimeOutLicencesService(
      licenceRepository,
      releaseDateService,
      auditEventRepository,
      licenceEventRepository,
    ),
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
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now())).thenReturn(true)

    service.timeOutLicencesJob()

    val cutOffDateCaptor = argumentCaptor<LocalDate>()

    verify(releaseDateService, times(0)).getCutOffDateForLicenceTimeOut(cutOffDateCaptor.capture())
    verify(licenceRepository, times(0)).getAllLicencesToBeTimeOut(cutOffDateCaptor.capture())
  }

  @Test
  fun `should not update licences status if there are no eligible licences`() {
    val jobExecutionDate = LocalDate.parse("2024-01-02")
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now())).thenReturn(false)
    whenever(releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now())).thenReturn(jobExecutionDate.plusDays(2))
    whenever(licenceRepository.getAllLicencesToBeTimeOut(LocalDate.now())).thenReturn(emptyList())

    service.timeOutLicencesJob()

    val jobExecutionDateCaptor = argumentCaptor<LocalDate>()
    val cutOffDateCaptor = argumentCaptor<LocalDate>()

    verify(releaseDateService, times(1)).getCutOffDateForLicenceTimeOut(jobExecutionDateCaptor.capture())
    verify(licenceRepository, times(1)).getAllLicencesToBeTimeOut(cutOffDateCaptor.capture())

    assertThat(jobExecutionDateCaptor.firstValue).isEqualTo(LocalDate.now())
    assertThat(cutOffDateCaptor.firstValue).isEqualTo(jobExecutionDate.plusDays(2))

    verify(licenceRepository, times(0)).saveAllAndFlush(emptyList())
  }

  @Test
  fun `should update licences status if there are eligible licences`() {
    val jobExecutionDate = LocalDate.parse("2024-01-02")
    whenever(releaseDateService.excludeBankHolidaysAndWeekends(LocalDate.now())).thenReturn(false)
    whenever(releaseDateService.getCutOffDateForLicenceTimeOut(LocalDate.now())).thenReturn(jobExecutionDate.plusDays(2))
    whenever(licenceRepository.getAllLicencesToBeTimeOut(jobExecutionDate.plusDays(2))).thenReturn(listOf(aLicenceEntity))

    service.timeOutLicencesJob()

    val jobExecutionDateCaptor = argumentCaptor<LocalDate>()
    val cutOffDateCaptor = argumentCaptor<LocalDate>()
    val licenceCaptor = argumentCaptor<List<Licence>>()

    verify(releaseDateService, times(1)).getCutOffDateForLicenceTimeOut(jobExecutionDateCaptor.capture())
    verify(licenceRepository, times(1)).getAllLicencesToBeTimeOut(cutOffDateCaptor.capture())

    assertThat(jobExecutionDateCaptor.firstValue).isEqualTo(LocalDate.now())
    assertThat(cutOffDateCaptor.firstValue).isEqualTo(jobExecutionDate.plusDays(2))

    verify(licenceRepository, times(1)).saveAllAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.firstValue[0])
      .extracting("statusCode", "updatedByUsername")
      .isEqualTo(listOf(LicenceStatus.TIME_OUT, "SYSTEM"))
  }

  private companion object {
    val someEntityStandardConditions = listOf(
      EntityStandardCondition(
        id = 1,
        conditionCode = "goodBehaviour",
        conditionSequence = 1,
        conditionText = "Be of good behaviour",
        conditionType = "AP",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 2,
        conditionCode = "notBreakLaw",
        conditionSequence = 2,
        conditionText = "Do not break any law",
        conditionType = "AP",
        licence = mock(),
      ),
      EntityStandardCondition(
        id = 3,
        conditionCode = "attendMeetings",
        conditionSequence = 3,
        conditionText = "Attend meetings",
        conditionType = "PSS",
        licence = mock(),
      ),
    )

    val aLicenceEntity = EntityLicence(
      id = 1,
      typeCode = LicenceType.AP,
      version = "1.1",
      statusCode = LicenceStatus.IN_PROGRESS,
      nomsId = "A1234AA",
      bookingNo = "123456",
      bookingId = 54321,
      crn = "X12345",
      pnc = "2019/123445",
      cro = "12345",
      prisonCode = "MDI",
      prisonDescription = "Moorland (HMP)",
      forename = "Bob",
      surname = "Mortimer",
      dateOfBirth = LocalDate.of(1985, 12, 28),
      conditionalReleaseDate = LocalDate.of(2021, 10, 22),
      actualReleaseDate = LocalDate.of(2021, 10, 22),
      sentenceStartDate = LocalDate.of(2018, 10, 22),
      sentenceEndDate = LocalDate.of(2021, 10, 22),
      licenceStartDate = LocalDate.of(2021, 10, 22),
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionStartDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      probationAreaCode = "N01",
      probationAreaDescription = "Wales",
      probationPduCode = "N01A",
      probationPduDescription = "Cardiff",
      probationLauCode = "N01A2",
      probationLauDescription = "Cardiff South",
      probationTeamCode = "NA01A2-A",
      probationTeamDescription = "Cardiff South Team A",
      dateCreated = LocalDateTime.of(2022, 7, 27, 15, 0, 0),
      standardConditions = someEntityStandardConditions,
      responsibleCom = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "smills",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
    )
  }
}
