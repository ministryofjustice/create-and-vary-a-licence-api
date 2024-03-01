package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.StandardCondition as EntityStandardCondition

class AppointmentServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditService = mock<AuditService>()
  private val staffRepository = mock<StaffRepository>()

  private val service = AppointmentService(licenceRepository, auditService, staffRepository)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("smills")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      auditService,
      staffRepository,
    )
  }

  @Test
  fun `update initial appointment person persists updated entity correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    service.updateAppointmentPerson(
      1L,
      AppointmentPersonRequest(
        appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
        appointmentPerson = "John Smith",
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentPersonType", "appointmentPerson", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(AppointmentPersonType.SPECIFIC_PERSON, "John Smith", aCom.username, aCom))
  }

  @Test
  fun `update initial appointment person throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentPerson(
        1L,
        AppointmentPersonRequest(
          appointmentPersonType = AppointmentPersonType.SPECIFIC_PERSON,
          appointmentPerson = "John Smith",
        ),
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
  }

  @Test
  fun `update initial appointment time persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    service.updateAppointmentTime(
      1L,
      AppointmentTimeRequest(
        appointmentTime = tenDaysFromNow,
        appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentTime", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf(tenDaysFromNow, aCom.username, aCom))
  }

  @Test
  fun `update initial appointment time throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentTime(
        1L,
        AppointmentTimeRequest(
          appointmentTime = tenDaysFromNow,
          appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
        ),
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verifyNoInteractions(staffRepository)
  }

  @Test
  fun `update contact number persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    service.updateContactNumber(1L, ContactNumberRequest(telephone = "0114 2565555"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentContact", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf("0114 2565555", aCom.username, aCom))
  }

  @Test
  fun `update contact number throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateContactNumber(1L, ContactNumberRequest(telephone = "0114 2565555"))
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verifyNoInteractions(staffRepository)
  }

  @Test
  fun `update appointment address persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("smills")).thenReturn(aCom)

    service.updateAppointmentAddress(
      1L,
      AppointmentAddressRequest(appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE"),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    assertThat(licenceCaptor.value)
      .extracting("appointmentAddress", "updatedByUsername", "updatedBy")
      .isEqualTo(listOf("221B Baker Street, London, City of London, NW1 6XE", aCom.username, aCom))
  }

  @Test
  fun `update appointment address throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentAddress(
        1L,
        AppointmentAddressRequest(appointmentAddress = "221B Baker Street, London, City of London, NW1 6XE"),
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verifyNoInteractions(staffRepository)
  }

  private companion object {
    val tenDaysFromNow: LocalDateTime = LocalDateTime.now().plusDays(10)

    val aLicenceEntity = TestData.createCrdLicence().copy(
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
      standardConditions = emptyList(),
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
      approvedByName = "jim smith",
      approvedDate = LocalDateTime.of(2023, 9, 19, 16, 38, 42),
    ).let {
      it.copy(
        standardConditions = listOf(
          EntityStandardCondition(
            id = 1,
            conditionCode = "goodBehaviour",
            conditionSequence = 1,
            conditionText = "Be of good behaviour",
            licence = it,
          ),
          EntityStandardCondition(
            id = 2,
            conditionCode = "notBreakLaw",
            conditionSequence = 2,
            conditionText = "Do not break any law",
            licence = it,
          ),
          EntityStandardCondition(
            id = 3,
            conditionCode = "attendMeetings",
            conditionSequence = 3,
            conditionText = "Attend meetings",
            licence = it,
          ),
        ),
      )
    }

    val aCom = TestData.com()
  }
}
