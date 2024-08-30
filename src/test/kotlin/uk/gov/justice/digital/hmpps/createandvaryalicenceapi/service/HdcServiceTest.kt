package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewHours
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData

class HdcServiceTest {
  private val hdcApiClient = mock<HdcApiClient>()

  private val service =
    HdcService(
      hdcApiClient,
    )

  @BeforeEach
  fun reset() {
    reset(hdcApiClient)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully`() {
    whenever(hdcApiClient.getByBookingId(1L)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewHours).isEqualTo(aSetOfCurfewHours)
    verify(hdcApiClient, times(1)).getByBookingId(1L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there is no curfew address`() {
    whenever(hdcApiClient.getByBookingId(1L)).thenReturn(
      someHdcLicenceData.copy(
        curfewAddress = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isNull()
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewHours).isEqualTo(aSetOfCurfewHours)
    verify(hdcApiClient, times(1)).getByBookingId(1L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there are no first night curfew hours`() {
    whenever(hdcApiClient.getByBookingId(1L)).thenReturn(
      someHdcLicenceData.copy(
        firstNightCurfewHours = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isNull()
    assertThat(result?.curfewHours).isEqualTo(aSetOfCurfewHours)
    verify(hdcApiClient, times(1)).getByBookingId(1L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there are no curfew hours`() {
    whenever(hdcApiClient.getByBookingId(1L)).thenReturn(
      someHdcLicenceData.copy(
        curfewHours = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewHours).isNull()
    verify(hdcApiClient, times(1)).getByBookingId(1L)
  }

  @Test
  fun `getHdcLicenceData returns address details in HDC licence data successfully when the second line is not set`() {
    val anAddress = aCurfewAddress.copy(
      addressLine2 = null,
    )
    whenever(hdcApiClient.getByBookingId(1L)).thenReturn(
      someHdcLicenceData.copy(
        curfewAddress = anAddress,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(anAddress)
    verify(hdcApiClient, times(1)).getByBookingId(1L)
  }

  private companion object {
    val aCurfewAddress = CurfewAddress(
      "1 Test Street",
      "Test Area",
      "Test Town",
      "AB1 2CD",
    )

    val aSetOfFirstNightCurfewHours = FirstNight(
      "16:00",
      "08:00",
    )

    val aSetOfCurfewHours = CurfewHours(
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
      "20:00",
      "08:00",
    )

    val someHdcLicenceData = HdcLicenceData(
      aCurfewAddress,
      aSetOfFirstNightCurfewHours,
      aSetOfCurfewHours,
    )
  }
}
