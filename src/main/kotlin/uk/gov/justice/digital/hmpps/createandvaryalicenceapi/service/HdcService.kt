package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Staff
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.AddHdcCurfewAddressRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateFirstNightCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateWeeklyCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatusHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence as HdcLicenceEntity

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val licenceRepository: LicenceRepository,
  private val staffRepository: StaffRepository,
  private val auditService: AuditService,
  @param:Value("\${feature.toggle.hdc.enabled}") private val useCurrentHdcStatus: Boolean = false,
) {

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }

  fun getHdcStatus(records: List<PrisonerSearchPrisoner>) = getHdcStatus(records, { it.bookingId?.toLong() }, { it.homeDetentionCurfewEligibilityDate })

  fun <T> getHdcStatus(
    records: Collection<T>,
    bookingIdGetter: (T) -> Long?,
    hdcedGetter: (T) -> LocalDate?,
  ): HdcStatuses {
    val bookingsWithHdc = records.filter { hdcedGetter(it) != null }.mapNotNull { bookingIdGetter(it) }
    val hdcStatuses: List<HdcStatusHolder> = if (useCurrentHdcStatus) {
      hdcApiClient.getCurrentHdcStatuses(bookingsWithHdc)
    } else {
      prisonApiClient.getHdcStatuses(bookingsWithHdc)
    }
    return HdcStatuses(hdcStatuses)
  }

  fun isApprovedForHdc(bookingId: Long, hdced: LocalDate?) = if (hdced == null) false else prisonApiClient.getHdcStatus(bookingId).isApproved()

  @Transactional
  fun getHdcLicenceDataByBookingId(bookingId: Long): HdcLicence = this.hdcApiClient.getByBookingId(bookingId)

  fun checkEligibleForHdcLicence(nomisRecord: PrisonerSearchPrisoner, hdcLicenceData: HdcLicence?) {
    if (nomisRecord.homeDetentionCurfewActualDate == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as it is missing a HDCAD")
    }

    if (nomisRecord.homeDetentionCurfewEligibilityDate == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as it is missing a HDCED")
    }

    if (!isApprovedForHdc(nomisRecord.bookingId!!.toLong(), nomisRecord.homeDetentionCurfewEligibilityDate)) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as they are not approved for HDC")
    }

    if (hdcLicenceData?.curfewAddress == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as there is no curfew address")
    }

    if (hdcLicenceData.curfewTimes == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as curfew times are missing")
    }
  }

  @Transactional
  fun updateWeeklyCurfewTimes(
    licenceId: Long,
    request: UpdateWeeklyCurfewTimesRequest,
  ) {
    val licence = licenceRepository.findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") }

    val hdcCase = requireNotNull(licence as? HdcCase) {
      "Licence ${licence::class.simpleName} does not support weekly curfew updates"
    }

    val username = SecurityContextHolder.getContext().authentication?.name!!

    val staff = staffRepository.findByUsernameIgnoreCase(username)

    val weeklyCurfewTimes = request.weeklyCurfewTimes
      .transformToEntityWeeklyCurfewTimes()

    hdcCase.updateWeeklyCurfewTimes(weeklyCurfewTimes, staff)

    licenceRepository.saveAndFlush(licence)

    auditService.recordAuditEventUpdateHdcWeeklyCurfewTimes(
      licence,
      weeklyCurfewTimes,
      staff,
    )
  }

  @Transactional
  fun updateFirstNightCurfewTimes(
    licenceId: Long,
    request: UpdateFirstNightCurfewTimesRequest,
  ) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") } as HdcLicenceEntity

    val username = SecurityContextHolder.getContext().authentication?.name!!
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val entityFirstNightCurfewTimes = request.firstNightCurfewTimes.transformToEntityFirstNightCurfewTimes()

    auditService.recordAuditEventUpdateHdcFirstNightCurfewTimes(
      licenceEntity,
      entityFirstNightCurfewTimes,
      staffMember,
    )

    licenceEntity.updateFirstNightCurfewTimes(
      updatedFirstNightCurfewTimes = entityFirstNightCurfewTimes,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
  }

  @Transactional
  fun addHdcCurfewAddress(licenceId: Long, request: AddHdcCurfewAddressRequest) {
    val licence = licenceRepository.findById(licenceId)
      .orElseThrow { EntityNotFoundException("Hdc Licence $licenceId not found") } as HdcVariationLicence

    val staff = getStaffUser()

    val existingAddress = licence.curfewAddress

    val auditData = if (existingAddress == null) {
      createHdcCurfewAddress(licence, request, staff)
    } else {
      updateHdcCurfewAddress(existingAddress, request, staff)
    }

    licence.dateLastUpdated = LocalDateTime.now()
    licence.updatedByUsername = getUserName(staff)
    licence.updatedBy = staff

    auditService.recordAuditEventHdcCurfewAddressUpdate(
      licence,
      auditData,
      staff,
    )
  }

  private fun createHdcCurfewAddress(
    licence: HdcVariationLicence,
    request: AddHdcCurfewAddressRequest,
    staff: Staff?,
  ): Map<String, String> {
    val addressReq = request.address
      ?: throw ValidationException("Address must be provided when creating HDC curfew address")

    log.info(
      "Creating HDC curfew address for licenceId={}, uprn={}, postcode={}, staffId={}",
      licence.id,
      addressReq.uprn,
      addressReq.postcode,
      staff?.id ?: "none",
    )

    val reference = UUID.randomUUID().toString()
    val addressString = addressReq.toString()

    val entity = HdcCurfewAddress(
      licence = licence,
      reference = reference,
      uprn = addressReq.uprn,
      firstLine = addressReq.firstLine,
      secondLine = addressReq.secondLine,
      townOrCity = addressReq.townOrCity,
      county = addressReq.county,
      postcode = addressReq.postcode,
      source = addressReq.source,
      accommodationType = request.accommodationType,
      postReleaseResidentialChecksCompleted = request.postReleaseResidentialChecksCompleted,
      postReleaseResidentialChecksNotCompletedReason = request.postReleaseResidentialChecksNotCompletedReason,
    )

    licence.curfewAddress = entity

    return buildAuditDetails(
      field = "createHdcCurfewAddress",
      value = addressString,
    )
  }

  private fun updateHdcCurfewAddress(
    entity: HdcCurfewAddress,
    request: AddHdcCurfewAddressRequest,
    staff: Staff?,
  ): Map<String, String> {
    val previousAddress = entity.toString()

    request.address?.let { addressReq ->

      log.info(
        "Updating address for licenceId={}, uprn={}, postcode={}, staffId={}",
        entity.licence.id,
        addressReq.uprn,
        addressReq.postcode,
        staff?.id ?: "none",
      )

      entity.uprn = addressReq.uprn
      entity.firstLine = addressReq.firstLine
      entity.secondLine = addressReq.secondLine
      entity.townOrCity = addressReq.townOrCity
      entity.county = addressReq.county
      entity.postcode = addressReq.postcode
      entity.source = addressReq.source
    }

    log.info(
      "Updating residential checks for licenceId={}, completed={}, reason='{}', staffId={}",
      entity.licence.id,
      request.postReleaseResidentialChecksCompleted,
      request.postReleaseResidentialChecksNotCompletedReason,
      staff?.id ?: "none",
    )

    entity.accommodationType = request.accommodationType
    entity.postReleaseResidentialChecksCompleted =
      request.postReleaseResidentialChecksCompleted
    entity.postReleaseResidentialChecksNotCompletedReason =
      request.postReleaseResidentialChecksNotCompletedReason

    entity.lastUpdatedTimestamp = LocalDateTime.now()

    return buildAuditDetails(
      field = "updateHdcCurfewAddress",
      value = request.address?.toString() ?: "No address change",
      previousValue = previousAddress,
    )
  }

  private fun buildAuditDetails(
    field: String,
    value: String? = null,
    previousValue: String? = null,
  ): MutableMap<String, String> {
    val details = mutableMapOf<String, String>()
    details["field"] = field
    value?.let { details["value"] = it }
    previousValue?.let { details["previousValue"] = it }
    return details
  }

  private fun getStaffUser(): Staff? {
    val username = SecurityContextHolder.getContext().authentication?.name!!
    return staffRepository.findByUsernameIgnoreCase(username)
  }

  private fun getUserName(staff: Staff?) = staff?.username ?: SYSTEM_USER
}
