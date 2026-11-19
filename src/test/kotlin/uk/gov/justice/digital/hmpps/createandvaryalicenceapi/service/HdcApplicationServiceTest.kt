package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcApplication
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource.HDC_CREATION
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.AddressSource.MANUAL
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.AccommodationType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.CvlAccommodationType.RESIDENTIAL
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.address.hdc.HdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcApplicationRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewAddressRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse.HdcLicence
import java.time.LocalDateTime

class HdcApplicationServiceTest {
  private val hdcApplicationRepository = mock<HdcApplicationRepository>()
  private val hdcCurfewAddressRepository = mock<HdcCurfewAddressRepository>()
  private val hdcApiClient = mock<HdcApiClient>()
  private val service = HdcApplicationService(hdcApplicationRepository, hdcCurfewAddressRepository, hdcApiClient)

  @Test
  fun `populateOnCreation stores proposed type and curfew address`() {
    val licence = createHdcLicence()
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, curfewAddress = address, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateOnCreation(licence)

    argumentCaptor<HdcApplication>().apply {
      verify(hdcApplicationRepository).saveAndFlush(capture())
      assertThat(firstValue.licence).isSameAs(licence)
      assertThat(firstValue.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
      assertThat(firstValue.isPostponed).isFalse()
    }
    val curfewAddress = licence.curfewAddress!!
    assertThat(curfewAddress.licence).isSameAs(licence)
    assertThat(curfewAddress.accommodationType).isEqualTo(RESIDENTIAL)
    assertThat(curfewAddress.firstLine).isEqualTo(address.addressLine1)
    assertThat(curfewAddress.townOrCity).isEqualTo(address.townOrCity)
    assertThat(curfewAddress.postcode).isEqualTo(address.postcode)
    assertThat(curfewAddress.source).isEqualTo(HDC_CREATION)
    verifyNoInteractions(hdcCurfewAddressRepository)
  }

  @Test
  fun `populateOnCreation stores CAS2 proposed type without curfew address`() {
    val licence = createHdcLicence()
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, proposeAddressType = AccommodationType.CAS_2),
    )

    service.populateOnCreation(licence)

    argumentCaptor<HdcApplication>().apply {
      verify(hdcApplicationRepository).saveAndFlush(capture())
      assertThat(firstValue.licence).isSameAs(licence)
      assertThat(firstValue.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
      assertThat(firstValue.isPostponed).isFalse()
    }
    verifyNoInteractions(hdcCurfewAddressRepository)
  }

  @Test
  fun `populateApplicationOnEdit stores the proposed type without adding a curfew address`() {
    val licence = createHdcLicence()
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, curfewAddress = address, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateApplicationOnEdit(licence)

    argumentCaptor<HdcApplication>().apply {
      verify(hdcApplicationRepository).saveAndFlush(capture())
      assertThat(firstValue.licence).isSameAs(licence)
      assertThat(firstValue.isPostponed).isFalse()
      assertThat(firstValue.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    }
    verifyNoInteractions(hdcCurfewAddressRepository)
  }

  @Test
  fun `refreshCurfewAddress updates the existing address and preserves its source`() {
    val licence = createHdcLicence()
    val application = HdcApplication(licence, true, AccommodationType.CAS_2)
    licence.application = application
    val existingAddress = HdcCurfewAddress(
      licence = licence,
      reference = "existing-reference",
      firstLine = "Old Street",
      townOrCity = "Old Town",
      postcode = "OLD 1AA",
      source = MANUAL,
    )
    licence.curfewAddress = existingAddress
    val address = CurfewAddress("123 Test Street", "Flat 1", "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(
        status = HdcStatus.RISK_CHECKS_COMPLETE,
        curfewAddress = address,
        proposeAddressType = AccommodationType.RESIDENTIAL,
      ),
    )

    service.refreshCurfewAddress(licence)

    assertThat(application.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(application.isPostponed).isTrue()
    assertThat(application.dateLastUpdated).isAfter(LocalDateTime.MIN)
    assertThat(existingAddress.firstLine).isEqualTo("123 Test Street")
    assertThat(existingAddress.secondLine).isEqualTo("Flat 1")
    assertThat(existingAddress.townOrCity).isEqualTo("Test Town")
    assertThat(existingAddress.postcode).isEqualTo("AB1 2CD")
    assertThat(existingAddress.accommodationType).isEqualTo(RESIDENTIAL)
    assertThat(existingAddress.source).isEqualTo(MANUAL)
    assertThat(existingAddress.reference).isEqualTo("existing-reference")
    assertThat(existingAddress.lastUpdatedTimestamp).isAfter(LocalDateTime.MIN)
    verifyNoInteractions(hdcCurfewAddressRepository)
  }

  @Test
  fun `refreshCurfewAddress creates an HDC sourced address when one does not exist`() {
    val licence = createHdcLicence().copy(curfewAddress = null)
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, curfewAddress = address),
    )

    service.refreshCurfewAddress(licence)

    assertThat(licence.curfewAddress).isNotNull
    assertThat(licence.curfewAddress!!.source).isEqualTo(HDC_CREATION)
    assertThat(licence.curfewAddress!!.firstLine).isEqualTo("123 Test Street")
    verifyNoInteractions(hdcCurfewAddressRepository)
  }

  @Test
  fun `refreshCurfewAddress leaves the existing address when HDC has no address`() {
    val licence = createHdcLicence()
    val application = HdcApplication(licence, true, null)
    licence.application = application
    val existingAddress = HdcCurfewAddress(
      licence = licence,
      reference = "existing-reference",
      firstLine = "Existing Street",
      townOrCity = "Existing Town",
      postcode = "OLD 1AA",
      source = MANUAL,
    )
    licence.curfewAddress = existingAddress
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(
        status = HdcStatus.RISK_CHECKS_COMPLETE,
        proposeAddressType = AccommodationType.CAS_2,
      ),
    )

    service.refreshCurfewAddress(licence)

    assertThat(application.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
    assertThat(application.isPostponed).isTrue()
    assertThat(existingAddress.firstLine).isEqualTo("Existing Street")
    verifyNoInteractions(hdcCurfewAddressRepository)
  }
}
