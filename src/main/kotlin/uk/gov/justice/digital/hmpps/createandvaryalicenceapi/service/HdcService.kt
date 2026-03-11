package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateFirstNightCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateWeeklyCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatusHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.LocalDate
import java.time.LocalTime

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val licenceRepository: LicenceRepository,
  private val staffRepository: StaffRepository,
  private val auditService: AuditService,
  @param:Value("\${feature.toggle.hdc.enabled}") private val useCurrentHdcStatus: Boolean = false,
) {

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
  fun getHdcLicenceDataByBookingId(bookingId: Long): HdcLicenceData? {
    val licenceData = this.hdcApiClient.getByBookingId(bookingId)
    return licenceData
  }

  @Transactional
  fun getHdcLicenceData(licenceId: Long): HdcLicenceData? {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("No licence data found for $licenceId") } as HdcCase

    val licenceData = this.hdcApiClient.getByBookingId(licence.bookingId!!)

    val weeklyCurfewTimes = if (licence.weeklyCurfewTimes.isNotEmpty()) {
      licence.weeklyCurfewTimes.transformToModelWeeklyCurfewTimes()
    } else {
      licenceData.weeklyCurfewTimes
    }

    val curfewAddress = if (licence.curfewAddress != null) {
      transformToModelHdcCurfewAddress(licence.curfewAddress!!)
    } else {
      licenceData.curfewAddress
    }

    val firstNightCurfewTimes = if (licence.firstNightCurfewTimes != null) {
      licence.firstNightCurfewTimes?.transformToModelFirstNightCurfewTimes()
    } else {
      licenceData.firstNightCurfewTimes ?: DEFAULT_FIRST_NIGHT_HOURS
    }
//      licence.firstNightCurfewTimes?.transformToModelFirstNightCurfewTimes() ?: DEFAULT_FIRST_NIGHT_HOURS

    return HdcLicenceData(
      licenceId = licenceData.licenceId,
      curfewAddress = curfewAddress,
      firstNightCurfewTimes = firstNightCurfewTimes,
      weeklyCurfewTimes = weeklyCurfewTimes,
    )
  }

  fun checkEligibleForHdcLicence(nomisRecord: PrisonerSearchPrisoner, hdcLicenceData: HdcLicenceData?) {
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

    if (hdcLicenceData.weeklyCurfewTimes == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as curfew times are missing")
    }
  }

  @Transactional
  fun updateWeeklyCurfewTimes(licenceId: Long, request: UpdateWeeklyCurfewTimesRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") } as HdcLicence

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val entityWeeklyCurfewTimes =
      request.weeklyCurfewTimes.transformToEntityWeeklyCurfewTimes()

    licenceEntity.updateWeeklyCurfewTimes(
      updatedWeeklyCurfewTimes = entityWeeklyCurfewTimes,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventUpdateHdcWeeklyCurfewTimes(licenceEntity, entityWeeklyCurfewTimes, staffMember)
  }

  @Transactional
  fun updateFirstNightCurfewTimes(
    licenceId: Long,
    request: UpdateFirstNightCurfewTimesRequest,
  ) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") } as HdcLicence

    val username = SecurityContextHolder.getContext().authentication.name
    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val entityFirstNightCurfewTimes =
      request.firstNightCurfewTimes.transformToEntityFirstNightCurfewTimes()

    licenceEntity.updateFirstNightCurfewTimes(
      updatedFirstNightCurfewTimes = entityFirstNightCurfewTimes,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)

    auditService.recordAuditEventUpdateHdcFirstNightCurfewTimes(
      licenceEntity,
      entityFirstNightCurfewTimes,
      staffMember,
    )
  }

  companion object {
    val DEFAULT_FIRST_NIGHT_HOURS = CurfewTimes(
      fromTime = LocalTime.of(15, 0),
      untilTime = LocalTime.of(7, 0),
    )
  }
}
