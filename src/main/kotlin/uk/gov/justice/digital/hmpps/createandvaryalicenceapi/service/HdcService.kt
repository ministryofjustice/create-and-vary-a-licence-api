package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewTimesRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData

@Service
class HdcService(
  private val hdcApiClient: HdcApiClient,
  private val hdcCurfewTimesRepository: HdcCurfewTimesRepository,
  private val licenceRepository: LicenceRepository,
) {

  @Transactional
  fun getHdcLicenceData(licenceId: Long): HdcLicenceData? {
    val licence = licenceRepository
      .findById(licenceId)
      .orElseThrow { EntityNotFoundException("No licence data found for $licenceId") }

    println(licence)
    val curfewTimes = hdcCurfewTimesRepository.findByLicenceId(licenceId)
    println(curfewTimes)
    val licenceData = this.hdcApiClient.getByBookingId(licence.bookingId!!)
    println(licenceData)
    return if (curfewTimes.isNotEmpty()) {
      HdcLicenceData(
        licenceId = licenceData.licenceId,
        curfewAddress = licenceData.curfewAddress,
        firstNightCurfewHours = licenceData.firstNightCurfewHours,
        curfewTimes = curfewTimes.transformToModelCurfewTimes(),
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
}
