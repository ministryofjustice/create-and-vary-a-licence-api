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
    argumentCaptor<HdcCurfewAddress>().apply {
      verify(hdcCurfewAddressRepository).saveAndFlush(capture())
      assertThat(firstValue.licence).isSameAs(licence)
      assertThat(firstValue.accommodationType).isEqualTo(RESIDENTIAL)
      assertThat(firstValue.firstLine).isEqualTo(address.addressLine1)
      assertThat(firstValue.townOrCity).isEqualTo(address.townOrCity)
      assertThat(firstValue.postcode).isEqualTo(address.postcode)
      assertThat(firstValue.source).isEqualTo(HDC_CREATION)
    }
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
}
