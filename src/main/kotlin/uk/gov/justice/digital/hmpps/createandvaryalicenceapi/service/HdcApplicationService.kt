package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcApplication
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcApplicationRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewAddressRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import java.util.UUID

@Service
class HdcApplicationService(
  private val hdcApplicationRepository: HdcApplicationRepository,
  private val hdcCurfewAddressRepository: HdcCurfewAddressRepository,
  private val hdcApiClient: HdcApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  fun populateOnCreation(licence: HdcLicence) {
    log.debug("Fetching HDC data for licenceId={}, bookingId={}", licence.id, licence.bookingId)
    val hdcLicenceData = hdcApiClient.getByBookingId(licence.bookingId!!)
    val curfewAddress = hdcLicenceData.curfewAddress

    saveApplication(licence, hdcLicenceData.proposeAddressType)

    if (curfewAddress != null) {
      val curfewAddressEntity = HdcCurfewAddress(
        licence = licence,
        accommodationType = curfewAddress.curfewAddressType.cvlAccommodationType,
        firstLine = curfewAddress.addressLine1!!,
        secondLine = curfewAddress.addressLine2,
        townOrCity = curfewAddress.townOrCity!!,
        postcode = curfewAddress.postcode!!,
        source = AddressSource.HDC_CREATION,
        reference = UUID.randomUUID().toString(),
      )
      hdcCurfewAddressRepository.saveAndFlush(curfewAddressEntity)
    }
    log.debug(
      "HDC data saved for licenceId={}, proposedAccommodationType={}, curfewAddressPresent={}",
      licence.id,
      hdcLicenceData.proposeAddressType,
      curfewAddress != null,
    )
  }

  fun populateApplicationOnEdit(licence: HdcLicence) {
    val hdcLicenceData = hdcApiClient.getByBookingId(licence.bookingId!!)
    saveApplication(licence, hdcLicenceData.proposeAddressType)
  }

  private fun saveApplication(licence: HdcLicence, proposedAccommodationType: AccommodationType?) {
    hdcApplicationRepository.saveAndFlush(
      HdcApplication(
        licence = licence,
        isPostponed = false,
        proposedAccommodationType = proposedAccommodationType,
      ),
    )
  }
}
