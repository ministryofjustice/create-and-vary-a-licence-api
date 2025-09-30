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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anotherCommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AddressMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType.DUTY_OFFICER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType.SPECIFIC_PERSON
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence as EntityLicence

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

    whenever(authentication.name).thenReturn(aCom.username)
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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(aCom)

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
    whenever(staffRepository.findByUsernameIgnoreCase(aCom.username)).thenReturn(null)

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

    val aLicenceEntity = createCrdLicence()

    val aCom = communityOffenderManager()

    val aPreviousUser = anotherCommunityOffenderManager()
  }
}
