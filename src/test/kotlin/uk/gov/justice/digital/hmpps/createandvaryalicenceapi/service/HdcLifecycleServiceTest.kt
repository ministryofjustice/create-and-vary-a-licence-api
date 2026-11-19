package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcLicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.reponse.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDateTime

class HdcLifecycleServiceTest {
  private val hdcLicenceRepository = mock<HdcLicenceRepository>()
  private val hdcApiClient = mock<HdcApiClient>()
  private val service = HdcLifecycleService(hdcLicenceRepository, hdcApiClient)

  @Test
  fun `populateOnCreation stores proposed type and curfew address`() {
    val licence = createHdcLicence()
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, curfewAddress = address, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateOnCreation(licence)

    assertThat(licence.application).isNotNull
    assertThat(licence.application!!.licence).isSameAs(licence)
    assertThat(licence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(licence.application!!.isPostponed).isFalse()
    val curfewAddress = licence.curfewAddress!!
    assertThat(curfewAddress.licence).isSameAs(licence)
    assertThat(curfewAddress.accommodationType).isEqualTo(RESIDENTIAL)
    assertThat(curfewAddress.firstLine).isEqualTo(address.addressLine1)
    assertThat(curfewAddress.townOrCity).isEqualTo(address.townOrCity)
    assertThat(curfewAddress.postcode).isEqualTo(address.postcode)
    assertThat(curfewAddress.source).isEqualTo(HDC_CREATION)
  }

  @Test
  fun `populateOnCreation stores CAS2 proposed type without curfew address`() {
    val licence = createHdcLicence()
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, proposeAddressType = AccommodationType.CAS_2),
    )

    service.populateOnCreation(licence)

    assertThat(licence.application).isNotNull
    assertThat(licence.application!!.licence).isSameAs(licence)
    assertThat(licence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
    assertThat(licence.application!!.isPostponed).isFalse()
  }

  @Test
  fun `populateApplicationOnEdit stores the proposed type without adding a curfew address`() {
    val licence = createHdcLicence()
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, curfewAddress = address, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateApplicationOnEdit(licence)

    assertThat(licence.application).isNotNull
    assertThat(licence.application!!.licence).isSameAs(licence)
    assertThat(licence.application!!.isPostponed).isFalse()
    assertThat(licence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
  }

  @Test
  fun `populateApplicationOnEdit updates an existing application without resetting postponement`() {
    val licence = createHdcLicence()
    val application = HdcApplication(licence, isPostponed = true, proposedAccommodationType = AccommodationType.CAS_2)
    licence.application = application
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateApplicationOnEdit(licence)

    assertThat(licence.application).isSameAs(application)
    assertThat(application.isPostponed).isTrue()
    assertThat(application.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(application.dateLastUpdated).isNotNull()
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
  }

  @Test
  fun `refreshCurfewAddress creates an HDC sourced address when one does not exist`() {
    val licence = createHdcLicence().copy(curfewAddress = null)
    licence.application = HdcApplication(licence, false, null)
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, curfewAddress = address),
    )

    service.refreshCurfewAddress(licence)

    assertThat(licence.curfewAddress).isNotNull
    assertThat(licence.curfewAddress!!.source).isEqualTo(HDC_CREATION)
    assertThat(licence.curfewAddress!!.firstLine).isEqualTo("123 Test Street")
  }

  @Test
  fun `refreshCurfewAddress fails when application is missing`() {
    val licence = createHdcLicence(id = 123L)

    assertThatThrownBy { service.refreshCurfewAddress(licence) }
      .isInstanceOf(IllegalArgumentException::class.java)
      .hasMessage("HDC application not found for licence 123")
    verifyNoInteractions(hdcApiClient)
  }

  @Test
  fun `refreshes the HDC curfew address for every in-flight licence`() {
    val firstLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", bookingId = 123L)
    val secondLicence = createHdcLicence(id = 2).copy(nomsId = "A1234BC", bookingId = 456L, statusCode = SUBMITTED)
    firstLicence.application = HdcApplication(firstLicence, true, null)
    secondLicence.application = HdcApplication(secondLicence, false, null)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(firstLicence, secondLicence))
    whenever(hdcApiClient.getByBookingId(123L)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, proposeAddressType = AccommodationType.RESIDENTIAL),
    )
    whenever(hdcApiClient.getByBookingId(456L)).thenReturn(
      HdcLicence(status = HdcStatus.RISK_CHECKS_COMPLETE, proposeAddressType = AccommodationType.CAS_2),
    )

    service.refreshHdcCurfewAddresses("A1234BC")

    assertThat(firstLicence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(secondLicence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
    verify(hdcApiClient).getByBookingId(123L)
    verify(hdcApiClient).getByBookingId(456L)
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
  }
}
