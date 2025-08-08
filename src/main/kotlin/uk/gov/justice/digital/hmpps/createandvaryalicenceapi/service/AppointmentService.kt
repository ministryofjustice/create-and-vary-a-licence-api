package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentPersonRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.AppointmentTimeRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ContactNumberRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.mapper.AddressMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentPersonType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.AppointmentTimeType
import java.time.LocalDateTime

@Service
class AppointmentService(
  private val licenceRepository: LicenceRepository,
  private val auditService: AuditService,
  private val staffRepository: StaffRepository,
  private val addressMapper: AddressMapper,
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

    val staffMember = getStaffUser()

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

    val staffMember = getStaffUser()

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

    val staffMember = getStaffUser()

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

  @Deprecated(" Use updateAppointmentAddress(licenceId: Long, request: AddAddressRequest) ")
  @Transactional
  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousAddress = licenceEntity.appointmentAddress
    val staffMember = getStaffUser()

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

  @Transactional
  fun addAppointmentAddress(licenceId: Long, request: AddAddressRequest) {
    val licence = licenceRepository.findById(licenceId)
      .orElseThrow { EntityNotFoundException("Licence $licenceId not found") }

    val staff = getStaffUser()
    val auditData = when (licence.licenceAppointmentAddress) {
      null -> createAppointmentAddress(licence, request, staff)
      else -> updateAppointmentAddress(licence, request, staff)
    }

    licence.dateLastUpdated = LocalDateTime.now()
    licence.updatedByUsername = getUserName(staff)
    licence.updatedBy = staff

    auditService.recordAuditEventInitialAppointmentUpdate(
      licence,
      auditData,
      staff,
    )
  }

  private fun createAppointmentAddress(licence: Licence, request: AddAddressRequest, staff: Staff?): Map<String, String> {
    val address = addressMapper.toEntity(request)
    val addressString = request.toString()

    licence.appointmentAddress = addressString
    licence.licenceAppointmentAddress = address

    staff?.let {
      if (request.isPreferredAddress) {
        it.savedAppointmentAddresses.add(address)
      }
    }

    return buildAuditDetails(
      field = "appointmentAddress",
      value = addressString,
      staffUsername = getUserName(staff),
      isPreferred = request.isPreferredAddress,
    )
  }

  private fun updateAppointmentAddress(licence: Licence, request: AddAddressRequest, staff: Staff?): Map<String, String> {
    val address = licence.licenceAppointmentAddress!!
    val previousAddress = licence.appointmentAddress!!
    val newAddressString = request.toString()

    staff?.let {
      if (request.isPreferredAddress) {
        it.savedAppointmentAddresses.add(address)
      }
    }

    addressMapper.update(address, request)
    licence.appointmentAddress = newAddressString

    return buildAuditDetails(
      field = "updateAppointmentAddress",
      previousValue = previousAddress,
      value = newAddressString,
      staffUsername = getUserName(staff),
      isPreferred = request.isPreferredAddress,
    )
  }

  private fun getUserName(staff: Staff?) = staff?.username ?: SYSTEM_USER

  private fun buildAuditDetails(
    field: String,
    value: String? = null,
    previousValue: String? = null,
    staffUsername: String,
    isPreferred: Boolean = false,
  ): MutableMap<String, String> {
    val details = mutableMapOf<String, String>()
    details["field"] = field
    value?.let { details["value"] = it }
    previousValue?.let { details["previousValue"] = it }
    if (isPreferred) {
      details["savedToStaffMember"] = staffUsername
    }
    return details
  }

  private fun getStaffUser(): Staff? {
    val username = SecurityContextHolder.getContext().authentication.name
    return staffRepository.findByUsernameIgnoreCase(username)
  }
}
