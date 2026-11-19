package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcApplication
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import java.time.LocalDateTime
import java.util.UUID

@Service
class HdcApplicationService(
  private val hdcApiClient: HdcApiClient,
) {
  private val log = LoggerFactory.getLogger(this::class.java)

  @Transactional
  fun populateOnCreation(licence: HdcLicence) {
    log.debug("Fetching HDC data for licenceId={}, bookingId={}", licence.id, licence.bookingId)
    val hdcLicenceData = hdcApiClient.getByBookingId(licence.bookingId!!)
    val curfewAddress = hdcLicenceData.curfewAddress

    licence.application = HdcApplication(
      licence = licence,
      isPostponed = false,
      proposedAccommodationType = hdcLicenceData.proposeAddressType,
    )

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
      licence.curfewAddress = curfewAddressEntity
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
    licence.application = HdcApplication(
      licence = licence,
      isPostponed = false,
      proposedAccommodationType = hdcLicenceData.proposeAddressType,
    )
  }

  @Transactional
  fun refreshCurfewAddress(licence: HdcLicence) {
    val application = requireNotNull(licence.application) {
      "HDC application not found for licence ${licence.id}"
    }
    val hdcLicenceData = hdcApiClient.getByBookingId(licence.bookingId!!)
    application.proposedAccommodationType = hdcLicenceData.proposeAddressType
    application.dateLastUpdated = LocalDateTime.now()

    val curfewAddress = hdcLicenceData.curfewAddress ?: run {
      log.info("No HDC curfew address to refresh for licenceId={}", licence.id)
      return
    }

    val existingAddress = licence.curfewAddress
    if (existingAddress == null) {
      val newAddress = HdcCurfewAddress(
        licence = licence,
        accommodationType = curfewAddress.curfewAddressType.cvlAccommodationType,
        firstLine = curfewAddress.addressLine1!!,
        secondLine = curfewAddress.addressLine2,
        townOrCity = curfewAddress.townOrCity!!,
        postcode = curfewAddress.postcode!!,
        source = AddressSource.HDC_CREATION,
        reference = UUID.randomUUID().toString(),
      )
      licence.curfewAddress = newAddress
    } else {
      existingAddress.accommodationType = curfewAddress.curfewAddressType.cvlAccommodationType
      existingAddress.firstLine = curfewAddress.addressLine1!!
      existingAddress.secondLine = curfewAddress.addressLine2
      existingAddress.townOrCity = curfewAddress.townOrCity!!
      existingAddress.postcode = curfewAddress.postcode!!
      existingAddress.lastUpdatedTimestamp = LocalDateTime.now()
    }
    log.info("HDC curfew address refreshed for licenceId={}", licence.id)
  }
}
