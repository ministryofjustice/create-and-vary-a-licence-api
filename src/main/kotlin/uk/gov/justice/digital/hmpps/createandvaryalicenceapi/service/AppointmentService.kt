package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Appointment
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

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

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

    val previousPerson = licenceEntity.appointment?.person

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
        "newValue" to (licenceEntity.appointment?.person ?: ""),
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
    val previousTime = licenceEntity.appointment?.time

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
        "newValue" to (licenceEntity.appointment?.time ?: "").toString(),
      ),
      staffMember,
    )
  }

  @Transactional
  fun updateContactNumber(licenceId: Long, request: ContactNumberRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousContact = licenceEntity.appointment?.telephoneContactNumber
    val previousContactAlternative = licenceEntity.appointment?.alternativeTelephoneContactNumber

    val staffMember = getStaffUser()

    licenceEntity.updateAppointmentContactNumber(
      telephoneContactNumber = request.telephone,
      alternativeTelephoneContactNumber = request.telephoneAlternative,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)

    // Audit primary contact number
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentContact",
        "previousValue" to (previousContact ?: ""),
        "newValue" to (licenceEntity.appointment?.telephoneContactNumber ?: ""),
      ),
      staffMember,
    )

    // Audit alternative contact number if provided
    if (request.telephoneAlternative != null) {
      auditService.recordAuditEventInitialAppointmentUpdate(
        licenceEntity,
        mapOf(
          "field" to "appointmentAlternativeTelephoneNumber",
          "previousValue" to (previousContactAlternative ?: ""),
          "newValue" to (licenceEntity.appointment?.alternativeTelephoneContactNumber ?: ""),
        ),
        staffMember,
      )
    }
  }

  @Deprecated(" Use updateAppointmentAddress(licenceId: Long, request: AddAddressRequest) ")
  @Transactional
  fun updateAppointmentAddress(licenceId: Long, request: AppointmentAddressRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val previousAddress = licenceEntity.appointment?.addressText
    val staffMember = getStaffUser()

    licenceEntity.updateAppointmentAddress(
      appointmentAddressText = request.appointmentAddress,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventInitialAppointmentUpdate(
      licenceEntity,
      mapOf(
        "field" to "appointmentAddress",
        "previousValue" to (previousAddress ?: ""),
        "newValue" to (licenceEntity.appointment?.addressText ?: ""),
      ),
      staffMember,
    )
  }

  @Transactional
  fun addAppointmentAddress(licenceId: Long, request: AddAddressRequest) {
    val licence = licenceRepository.findById(licenceId)
      .orElseThrow { EntityNotFoundException("Licence $licenceId not found") }

    val staff = getStaffUser()
    val auditData = when (licence.appointment?.address) {
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
    log.info(
      "Creating appointment address for licenceId={}, prn={}, postcode={}, staffId={}",
      licence.id,
      request.uprn,
      request.postcode,
      staff?.id ?: "none",
    )

    val addressString = request.toString()

    if (licence.appointment == null) {
      licence.appointment = Appointment()
    }
    with(licence.appointment!!) {
      addressText = addressString
      address = addressMapper.toEntity(request)
    }
    addPreferredAddress(staff, request)

    return buildAuditDetails(
      field = "appointmentAddress",
      value = addressString,
      staffUsername = getUserName(staff),
      isPreferred = request.isPreferredAddress,
    )
  }

  private fun updateAppointmentAddress(licence: Licence, request: AddAddressRequest, staff: Staff?): Map<String, String> {
    log.info(
      "Updating appointment address for licenceId={}, prn={}, postcode={}, staffId={}",
      licence.id,
      request.uprn,
      request.postcode,
      staff?.id ?: "none",
    )

    val address = licence.appointment!!.address!!
    val previousAddress = licence.appointment?.addressText ?: ""
    val newAddressString = request.toString()

    addPreferredAddress(staff, request)

    addressMapper.update(address, request)
    if (licence.appointment == null) {
      licence.appointment = Appointment()
    }
    licence.appointment?.addressText = newAddressString

    return buildAuditDetails(
      field = "updateAppointmentAddress",
      previousValue = previousAddress,
      value = newAddressString,
      staffUsername = getUserName(staff),
      isPreferred = request.isPreferredAddress,
    )
  }

  private fun addPreferredAddress(
    staff: Staff?,
    request: AddAddressRequest,
  ) {
    if (request.isPreferredAddress) {
      // Create a new address entity not connected to the licence as they different relationships
      // and are managed separately!
      val newPreferredAddress = addressMapper.toEntity(request)
      staff?.let {
        val exists = it.savedAppointmentAddresses.any { existing -> existing.isSame(newPreferredAddress) }
        if (!exists) {
          it.savedAppointmentAddresses.add(newPreferredAddress)
        }
      }
    }
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
