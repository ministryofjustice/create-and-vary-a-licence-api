package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewTimesRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.transformToModelCurfewTimes

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val hdcCurfewTimesRepository: HdcCurfewTimesRepository,
) {

  @Transactional
  fun getHdcLicenceData(bookingId: Long): HdcLicenceData? {
    val licenceData = this.hdcApiClient.getByBookingId(bookingId)

    val curfewTimes = hdcCurfewTimesRepository.findByLicenceId(licenceData.licenceId)

    return if (curfewTimes.isEmpty()) {
      HdcLicenceData(
        licenceId = licenceData.licenceId,
        curfewAddress = licenceData.curfewAddress,
        firstNightCurfewHours = licenceData.firstNightCurfewHours,
        curfewTimes = licenceData.curfewTimes,
      )
    } else {
      HdcLicenceData(
        licenceId = licenceData.licenceId,
        curfewAddress = licenceData.curfewAddress,
        firstNightCurfewHours = licenceData.firstNightCurfewHours,
        curfewTimes = curfewTimes.transformToModelCurfewTimes(),
      )
    }
  }
}
