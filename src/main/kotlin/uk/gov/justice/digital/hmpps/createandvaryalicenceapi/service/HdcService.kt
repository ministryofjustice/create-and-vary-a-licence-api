package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
) {

  @Transactional
  fun getHdcLicenceData(bookingId: String?): HdcLicenceData? {
    if (bookingId == null) {
      return null
    }

    val licenceData = this.hdcApiClient.getByBookingId(bookingId.toLong())

    return HdcLicenceData(
      curfewAddress = licenceData?.curfewAddress,
      firstNightCurfewHours = licenceData?.firstNightCurfewHours,
      curfewHours = licenceData?.curfewHours,
    )
  }
}
