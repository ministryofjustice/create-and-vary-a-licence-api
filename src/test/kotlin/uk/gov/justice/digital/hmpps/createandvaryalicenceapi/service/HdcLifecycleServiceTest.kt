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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence as HdcLicenceEntity

class HdcLifecycleServiceTest {
  private val hdcLicenceRepository = mock<HdcLicenceRepository>()
  private val hdcApiClient = mock<HdcApiClient>()
  private val auditService = mock<AuditService>()
  private val service = HdcLifecycleService(hdcLicenceRepository, hdcApiClient, auditService)

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
  fun `populateApplicationOnEdit creates an application for the new edit licence`() {
    val original = createHdcLicence()
    val originalApplication = HdcApplication(original, isPostponed = true, proposedAccommodationType = AccommodationType.CAS_2)
    original.application = originalApplication
    val licence = original.copy(id = 2L)
    assertThat(licence.application).isNull()
    val address = CurfewAddress("123 Test Street", null, "Test Town", "AB1 2CD", AccommodationType.RESIDENTIAL)
    whenever(hdcApiClient.getByBookingId(licence.bookingId!!)).thenReturn(
      HdcLicence(status = HdcStatus.APPROVED, curfewAddress = address, proposeAddressType = AccommodationType.RESIDENTIAL),
    )

    service.populateApplicationOnEdit(licence)

    assertThat(licence.application).isNotNull
    assertThat(licence.application!!.licence).isSameAs(licence)
    assertThat(licence.application!!.isPostponed).isFalse()
    assertThat(licence.application!!.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(original.application).isSameAs(originalApplication)
    assertThat(originalApplication.isPostponed).isTrue()
    assertThat(originalApplication.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
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
  fun `withdraws curfew addresses while preserving applications for every in-flight licence`() {
    val firstLicence = createHdcLicence(id = 1).copy(nomsId = "A1234BC", statusCode = APPROVED)
    val secondLicence = createHdcLicence(id = 2).copy(nomsId = "A1234BC", statusCode = SUBMITTED)
    val firstApplication = HdcApplication(firstLicence, isPostponed = true, AccommodationType.RESIDENTIAL)
    val secondApplication = HdcApplication(secondLicence, isPostponed = false, AccommodationType.CAS_2)
    val lastUpdated = LocalDateTime.of(2024, 1, 1, 12, 0)
    firstApplication.dateLastUpdated = lastUpdated
    secondApplication.dateLastUpdated = lastUpdated
    firstLicence.application = firstApplication
    secondLicence.application = secondApplication
    firstLicence.curfewAddress = createCurfewAddress(firstLicence)
    secondLicence.curfewAddress = createCurfewAddress(secondLicence)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(firstLicence, secondLicence))

    service.withdrawHdcCurfewAddresses("A1234BC")

    assertThat(firstLicence.curfewAddress).isNull()
    assertThat(secondLicence.curfewAddress).isNull()
    assertThat(firstApplication.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(secondApplication.proposedAccommodationType).isEqualTo(AccommodationType.CAS_2)
    assertThat(firstApplication.isPostponed).isTrue()
    assertThat(secondApplication.isPostponed).isFalse()
    assertThat(firstApplication.dateLastUpdated).isEqualTo(lastUpdated)
    assertThat(secondApplication.dateLastUpdated).isEqualTo(lastUpdated)
    verify(auditService).recordAuditEventHdcCurfewAddressWithdrawn(firstLicence.id)
    verify(auditService).recordAuditEventHdcCurfewAddressWithdrawn(secondLicence.id)
  }

  @Test
  fun `withdrawal without an address leaves the application unchanged and does not audit`() {
    val licence = createHdcLicence(id = 123L).copy(nomsId = "A1234BC")
    val application = HdcApplication(licence, false, AccommodationType.RESIDENTIAL)
    val lastUpdated = LocalDateTime.of(2024, 1, 1, 12, 0)
    application.dateLastUpdated = lastUpdated
    licence.application = application
    licence.curfewAddress = null
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(licence))

    service.withdrawHdcCurfewAddresses("A1234BC")

    assertThat(application.proposedAccommodationType).isEqualTo(AccommodationType.RESIDENTIAL)
    assertThat(application.dateLastUpdated).isEqualTo(lastUpdated)
    verifyNoInteractions(auditService)
  }

  @Test
  fun `withdraws an address when an in-flight licence has no HDC application`() {
    val licence = createHdcLicence(id = 123L).copy(nomsId = "A1234BC")
    licence.curfewAddress = createCurfewAddress(licence)
    whenever(hdcLicenceRepository.findInFlightHdcLicences("A1234BC")).thenReturn(listOf(licence))

    service.withdrawHdcCurfewAddresses("A1234BC")

    assertThat(licence.curfewAddress).isNull()
    verify(auditService).recordAuditEventHdcCurfewAddressWithdrawn(licence.id)
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

  private fun createCurfewAddress(licence: HdcLicenceEntity) = HdcCurfewAddress(
    licence = licence,
    reference = "ref-${licence.id}",
    firstLine = "Test Street",
    townOrCity = "Test Town",
    postcode = "TE1 1ST",
    source = HDC_CREATION,
  )
}
