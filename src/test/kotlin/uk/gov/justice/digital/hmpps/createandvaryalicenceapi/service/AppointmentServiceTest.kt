package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AddressMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType.DUTY_OFFICER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType.SPECIFIC_PERSON
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
  private val addressMapper = AddressMapper()

  private val service = AppointmentService(licenceRepository, auditService, staffRepository, addressMapper)

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()

    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)
    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      auditService,
      staffRepository,
    )
  }

  @Test
  fun `update initial appointment person validates that person is populated`() {
    val exception = assertThrows<ValidationException> {
      service.updateAppointmentPerson(1L, AppointmentPersonRequest(SPECIFIC_PERSON, ""))
    }

    assertThat(exception.message).isEqualTo("Appointment person must not be empty if Appointment With Type is SPECIFIC_PERSON")
  }

  @Test
  fun `update initial appointment person persists updated entity correctly`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

    service.updateAppointmentPerson(
      1L,
      AppointmentPersonRequest(
        appointmentPersonType = SPECIFIC_PERSON,
        appointmentPerson = "John Smith",
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    val licence = licenceCaptor.value
    val appointment = licence.appointment
    assertThat(appointment).isNotNull
    assertThat(appointment!!.personType).isEqualTo(SPECIFIC_PERSON)
    assertThat(appointment.person).isEqualTo("John Smith")
    assertThat(licence.updatedByUsername).isEqualTo(aCom.username)
    assertThat(licence.updatedBy!!.username).isEqualTo(aCom.username)
  }

  @Test
  fun `update initial appointment clears specific person if not a appointment type is not with a specific person `() {
    // Given
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity.copy(appointment = TestData.createAppointment())))
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

    // When
    service.updateAppointmentPerson(
      1L,
      AppointmentPersonRequest(
        appointmentPersonType = DUTY_OFFICER,
        appointmentPerson = "John Smith",
      ),
    )

    // Then
    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    val licence = licenceCaptor.value
    val appointment = licence.appointment
    assertThat(appointment).isNotNull
    assertThat(appointment!!.personType).isEqualTo(DUTY_OFFICER)
    assertThat(appointment.person).isNull()
    assertThat(licence.updatedByUsername).isEqualTo(aCom.username)
    assertThat(licence.updatedBy).isEqualTo(aCom)
  }

  @Test
  fun `update initial appointment person throws not found exception if licence not found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.empty())

    val exception = assertThrows<EntityNotFoundException> {
      service.updateAppointmentPerson(
        1L,
        AppointmentPersonRequest(
          appointmentPersonType = SPECIFIC_PERSON,
          appointmentPerson = "John Smith",
        ),
      )
    }

    assertThat(exception).isInstanceOf(EntityNotFoundException::class.java)

    verify(licenceRepository, times(1)).findById(1L)
    verifyNoInteractions(staffRepository)
  }

  @Test
  fun `update initial appointment time persists the updated entity`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

    service.updateAppointmentTime(
      1L,
      AppointmentTimeRequest(
        appointmentTime = tenDaysFromNow,
        appointmentTimeType = AppointmentTimeType.SPECIFIC_DATE_TIME,
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    val licence = licenceCaptor.value
    val appointment = licence.appointment
    assertThat(appointment).isNotNull
    assertThat(appointment!!.time).isEqualTo(tenDaysFromNow)
    assertThat(licence.updatedByUsername).isEqualTo(aCom.username)
    assertThat(licence.updatedBy!!.username).isEqualTo(aCom.username)
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
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

    service.updateContactNumber(1L, ContactNumberRequest(telephone = "0114 2565555"))

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    val licence = licenceCaptor.value
    val appointment = licence.appointment
    assertThat(appointment).isNotNull
    assertThat(appointment!!.telephoneContactNumber).isEqualTo("0114 2565555")
    assertThat(licence.updatedByUsername).isEqualTo(aCom.username)
    assertThat(licence.updatedBy!!.username).isEqualTo(aCom.username)
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
  fun `updating user is retained and username is set to SYSTEM_USER when a staff member cannot be found`() {
    whenever(licenceRepository.findById(1L)).thenReturn(
      Optional.of(
        aLicenceEntity.copy(
          updatedBy = aPreviousUser,
        ),
      ),
    )
    whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(null)

    service.updateAppointmentPerson(
      1L,
      AppointmentPersonRequest(
        appointmentPersonType = SPECIFIC_PERSON,
        appointmentPerson = "John Smith",
      ),
    )

    val licenceCaptor = ArgumentCaptor.forClass(EntityLicence::class.java)
    verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())

    val licence = licenceCaptor.value
    val appointment = licence.appointment
    assertThat(appointment).isNotNull
    assertThat(appointment!!.personType).isEqualTo(SPECIFIC_PERSON)
    assertThat(appointment.person).isEqualTo("John Smith")
    assertThat(licence.updatedByUsername).isEqualTo(SYSTEM_USER)
    assertThat(licence.updatedBy).isEqualTo(aPreviousUser)
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
      forename = "Person",
      surname = "One",
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
        username = "tcom",
        email = "testemail@probation.gov.uk",
        firstName = "X",
        lastName = "Y",
      ),
      createdBy = CommunityOffenderManager(
        staffIdentifier = 2000,
        username = "tcom",
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
            conditionType = "AP",
            licence = it,
          ),
          EntityStandardCondition(
            id = 2,
            conditionCode = "notBreakLaw",
            conditionSequence = 2,
            conditionText = "Do not break any law",
            conditionType = "AP",
            licence = it,
          ),
          EntityStandardCondition(
            id = 3,
            conditionCode = "attendMeetings",
            conditionSequence = 3,
            conditionText = "Attend meetings",
            conditionType = "AP",
            licence = it,
          ),
        ),
      )
    }

    val aCom = TestData.com()

    val aPreviousUser = CommunityOffenderManager(
      staffIdentifier = 4000,
      username = "test",
      email = "test@test.com",
      firstName = "Test",
      lastName = "Test",
    )
  }
}
