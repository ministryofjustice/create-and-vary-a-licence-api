package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
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
) {

  fun getHdcStatus(records: List<PrisonerSearchPrisoner>) = getHdcStatus(records, { it.bookingId?.toLong() }, { it.homeDetentionCurfewEligibilityDate })

  fun <T> getHdcStatus(
    records: Collection<T>,
    bookingIdGetter: (T) -> Long?,
    hdcedGetter: (T) -> LocalDate?,
  ): HdcStatuses {
    val bookingsWithHdc = records.filter { hdcedGetter(it) != null }.mapNotNull { bookingIdGetter(it) }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return HdcStatuses(hdcStatuses.filter { it.isApproved() }.mapNotNull { it.bookingId }.toSet())
  }

  fun isApprovedForHdc(bookingId: Long, hdced: LocalDate?) = if (hdced == null) false else prisonApiClient.getHdcStatus(bookingId).isApproved()

  @Transactional
  fun getHdcLicenceData(licenceId: Long): HdcLicenceData? {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("No licence data found for $licenceId") } as HdcLicence

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

  fun isEligibleForHdcLicence(nomisRecord: PrisonerSearchPrisoner): Boolean {
    if (nomisRecord.homeDetentionCurfewActualDate == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as it is missing a HDCAD")
    }

    if (nomisRecord.homeDetentionCurfewEligibilityDate == null) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as it is missing a HDCED")
    }

    if (!isApprovedForHdc(nomisRecord.bookingId!!.toLong(), nomisRecord.homeDetentionCurfewEligibilityDate)) {
      error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as they are not approved for HDC")
    }

    hdcApiClient.getByBookingId(nomisRecord.bookingId!!.toLong()).curfewAddress
      ?: error("HDC licence for ${nomisRecord.prisonerNumber} could not be created as there is no curfew address")

    return true
  }

  data class HdcStatuses(val approvedIds: Set<Long>) {
    fun isWaitingForActivation(kind: LicenceKind, bookingId: Long) = kind == HDC && !approvedIds.contains(bookingId)

    fun canBeActivated(kind: LicenceKind, bookingId: Long) = isValidByKind(kind, bookingId)

    /**
     * For COM:
     * If licence hasn't been started base on whether licence is approved.
     * TODO: will need to fix when we start to show prospective HDC cases in caselist
     * If licence has been started then show when (approved and HDC) or (!approved and !hdc)
     */
    fun canBeSeenByCom(kind: LicenceKind?, bookingId: Long) = isValidByKind(kind, bookingId)

    /**
     * For CA:
     * Always show started cases regardless of HDC status.
     * TODO: will need to fix when we start to show prospective HDC cases in caselist
     * If licence hasn't been started, only show !approved licences.
     */
    fun canUnstartedCaseBeSeenByCa(bookingId: Long) = !isApprovedForHdc(bookingId)

    fun isApprovedForHdc(bookingId: Long) = approvedIds.contains(bookingId)

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
