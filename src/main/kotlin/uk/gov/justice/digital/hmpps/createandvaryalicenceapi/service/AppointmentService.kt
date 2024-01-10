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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType

@Service
class AppointmentService(
  private val licenceRepository: LicenceRepository,
  private val auditService: AuditService,
) {
  @Transactional
  fun updateAppointmentPerson(licenceId: Long, request: AppointmentPersonRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousPerson = licenceEntity.appointmentPerson

    val updatedLicence = licenceEntity.updateAppointmentPerson(
      appointmentPerson = request.appointmentPerson,
      updatedByUsername = SecurityContextHolder.getContext().authentication.name,
    )
    licenceRepository.saveAndFlush(updatedLicence)
    auditService.recordAuditEventInitialAppointmentUpdate(
      updatedLicence,
      mapOf(
        "field" to "appointmentPerson",
        "previousValue" to (previousPerson ?: ""),
        "newValue" to (updatedLicence.appointmentPerson ?: ""),
      ),
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

    val updatedLicence = licenceEntity.updateAppointmentTime(
      appointmentTime = request.appointmentTime,
      appointmentTimeType = request.appointmentTimeType,
      updatedByUsername = SecurityContextHolder.getContext().authentication.name,
    )
    licenceRepository.saveAndFlush(updatedLicence)
    auditService.recordAuditEventInitialAppointmentUpdate(
      updatedLicence,
      mapOf(
        "field" to "appointmentTime",
        "previousValue" to (previousTime ?: "").toString(),
        "newValue" to (updatedLicence.appointmentTime ?: "").toString(),
      ),
    )
  }

  @Transactional
  fun updateContactNumber(licenceId: Long, request: ContactNumberRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousContact = licenceEntity.appointmentContact

    val updatedLicence = licenceEntity.updateAppointmentContactNumber(
      appointmentContact = request.telephone,
      updatedByUsername = SecurityContextHolder.getContext().authentication.name,
    )

    licenceRepository.saveAndFlush(updatedLicence)
    auditService.recordAuditEventInitialAppointmentUpdate(
      updatedLicence,
      mapOf(
        "field" to "appointmentContact",
        "previousValue" to (previousContact ?: ""),
        "newValue" to (updatedLicence.appointmentContact ?: ""),
      ),
    )
  }

  @Transactional
  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousAddress = licenceEntity.appointmentAddress

    val updatedLicence = licenceEntity.updateAppointmentAddress(
      appointmentAddress = request.appointmentAddress,
      updatedByUsername = SecurityContextHolder.getContext().authentication.name,
    )

    licenceRepository.saveAndFlush(updatedLicence)
    auditService.recordAuditEventInitialAppointmentUpdate(
      updatedLicence,
      mapOf(
        "field" to "appointmentAddress",
        "previousValue" to (previousAddress ?: ""),
        "newValue" to (updatedLicence.appointmentAddress ?: ""),
      ),
    )
  }
}
