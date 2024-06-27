package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType

@Service
class AppointmentService(
  private val licenceRepository: LicenceRepository,
  private val auditService: AuditService,
  private val staffRepository: StaffRepository,
) {
  @Transactional
  fun updateAppointmentPerson(licenceId: Long, request: AppointmentPersonRequest) {
    if (request.appointmentPersonType === AppointmentPersonType.SPECIFIC_PERSON) {
      if (request.appointmentPerson.isNullOrBlank()) {
        throw ValidationException("Appointment person must not be empty if Appointment With Type is SPECIFIC_PERSON")
      }
    }
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousPerson = licenceEntity.appointmentPerson

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateAppointmentPerson(
      appointmentPersonType = request.appointmentPersonType,
      appointmentPerson = request.appointmentPerson,
      staffMember = staffMember,
    )
    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentPerson",
        "previousValue" to (previousPerson ?: ""),
        "newValue" to (licenceEntity.appointmentPerson ?: ""),
      ),
      staffMember,
    )
  }

  @Transactional
  fun updateAppointmentTime(licenceId: Long, request: AppointmentTimeRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    if (request.appointmentTimeType === AppointmentTimeType.SPECIFIC_DATE_TIME) {
      if (request.appointmentTime == null) {
        throw ValidationException("Appointment time must not be null if Appointment Type is SPECIFIC_DATE_TIME")
      }
    }
    val previousTime = licenceEntity.appointmentTime

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateAppointmentTime(
      appointmentTime = request.appointmentTime,
      appointmentTimeType = request.appointmentTimeType,
      staffMember = staffMember,
    )
    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentTime",
        "previousValue" to (previousTime ?: "").toString(),
        "newValue" to (licenceEntity.appointmentTime ?: "").toString(),
      ),
      staffMember,
    )
  }

  @Transactional
  fun updateContactNumber(licenceId: Long, request: ContactNumberRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousContact = licenceEntity.appointmentContact

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateAppointmentContactNumber(
      appointmentContact = request.telephone,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentContact",
        "previousValue" to (previousContact ?: ""),
        "newValue" to (licenceEntity.appointmentContact ?: ""),
      ),
      staffMember,
    )
  }

  @Transactional
  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousAddress = licenceEntity.appointmentAddress

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    licenceEntity.updateAppointmentAddress(
      appointmentAddress = request.appointmentAddress,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentAddress",
        "previousValue" to (previousAddress ?: ""),
        "newValue" to (licenceEntity.appointmentAddress ?: ""),
      ),
      staffMember,
    )
  }
}
