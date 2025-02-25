package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import java.time.LocalDate

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val prisonApiClient: PrisonApiClient,
  private val licenceRepository: LicenceRepository,
) {

  fun <T> getHdcStatus(
    records: Collection<T>,
    bookingIdGetter: (T) -> Long?,
    hdcedGetter: (T) -> LocalDate?,
  ): HdcStatuses {
    val bookingsWithHdc = records.filter { hdcedGetter(it) != null }.mapNotNull { bookingIdGetter(it) }
    val hdcStatuses = prisonApiClient.getHdcStatuses(bookingsWithHdc)
    return HdcStatuses(hdcStatuses.filter { it.isApproved() }.mapNotNull { it.bookingId }.toSet())
  }

  @Transactional
  fun getHdcLicenceData(licenceId: Long): HdcLicenceData? {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("No licence data found for $licenceId") } as HdcLicence

    val licenceData = this.hdcApiClient.getByBookingId(licence.bookingId!!)

    return if (licence.curfewTimes.isNotEmpty()) {
      HdcLicenceData(
        licenceId = licenceData.licenceId,
        curfewAddress = licenceData.curfewAddress,
        firstNightCurfewHours = licenceData.firstNightCurfewHours,
        curfewTimes = licence.curfewTimes.transformToModelCurfewTimes(),
      )
    } else {
      HdcLicenceData(
        licenceId = licenceData.licenceId,
        curfewAddress = licenceData.curfewAddress,
        firstNightCurfewHours = licenceData.firstNightCurfewHours,
        curfewTimes = licenceData.curfewTimes,
      )
    }
  }

  data class HdcStatuses(val approvedIds: Set<Long>) {
    fun isWaitingForActivation(kind: LicenceKind, bookingId: Long) = kind == HDC && !approvedIds.contains(bookingId)

    fun canBeActivated(kind: LicenceKind, bookingId: Long): Boolean {
      val approvedForHdc = approvedIds.contains(bookingId)
      return (kind == HDC && approvedForHdc) || (kind != HDC && !approvedForHdc)
    }
  }
}
