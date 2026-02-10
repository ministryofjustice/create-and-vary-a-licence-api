package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.HdcStatusHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import java.time.LocalDate
import java.time.LocalTime

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val licenceRepository: LicenceRepository,
  private val staffRepository: StaffRepository,
  private val auditService: AuditService,
  @param:Value("\${feature.toggle.hdc.useCurrentStatus}") private val useCurrentHdcStatus: Boolean = false,
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
    return HdcStatuses(hdcStatuses.filter { it.isHdcRelease() }.mapNotNull { it.bookingId }.toSet())
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

    val curfewTimes = if (licence.curfewTimes.isNotEmpty()) {
      licence.curfewTimes.transformToModelCurfewTimes()
    } else {
      licenceData.curfewTimes
    }

    val curfewAddress = if (licence.curfewAddress != null) {
      transformToModelHdcCurfewAddress(licence.curfewAddress!!)
    } else {
      licenceData.curfewAddress
    }

    val firstNightCurfewHours = licenceData.firstNightCurfewHours ?: DEFAULT_FIRST_NIGHT_HOURS

    return HdcLicenceData(
      licenceId = licenceData.licenceId,
      curfewAddress = curfewAddress,
      firstNightCurfewHours = firstNightCurfewHours,
      curfewTimes = curfewTimes,
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

    if (hdcLicenceData.curfewTimes == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as curfew times are missing")
    }
  }

  @Transactional
  fun updateCurfewTimes(licenceId: Long, request: UpdateCurfewTimesRequest) {
    val licenceEntity = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("$licenceId") } as HdcLicence

    val username = SecurityContextHolder.getContext().authentication.name

    val staffMember = staffRepository.findByUsernameIgnoreCase(username)

    val entityCurfewTimes =
      request.curfewTimes.transformToEntityHdcCurfewTimes(licenceEntity)

    licenceEntity.updateCurfewTimes(
      updatedCurfewTimes = entityCurfewTimes,
      staffMember = staffMember,
    )

    licenceRepository.saveAndFlush(licenceEntity)
    auditService.recordAuditEventUpdateHdcCurfewTimes(licenceEntity, entityCurfewTimes, staffMember)
  }

  data class HdcStatuses(val approvedIds: Set<Long>) {
    fun isWaitingForActivation(kind: LicenceKind, bookingId: Long) = kind == HDC && !approvedIds.contains(bookingId)

    fun canBeActivated(kind: LicenceKind, bookingId: Long) = isValidByKind(kind, bookingId)

    fun isExpectedHdcRelease(bookingId: Long) = approvedIds.contains(bookingId)

    private fun isValidByKind(kind: LicenceKind?, bookingId: Long): Boolean {
      val approvedForHdc = approvedIds.contains(bookingId)
      return (kind == HDC && approvedForHdc) || (kind != HDC && !approvedForHdc)
    }
  }

  companion object {
    val DEFAULT_FIRST_NIGHT_HOURS = FirstNight(
      firstNightFrom = LocalTime.of(15, 0),
      firstNightUntil = LocalTime.of(7, 0),
    )
  }
}
