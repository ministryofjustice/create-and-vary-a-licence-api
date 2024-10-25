package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.HdcCurfewTimesRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes as EntityHdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes as ModelHdcCurfewTimes

class HdcServiceTest {
  private val hdcApiClient = mock<HdcApiClient>()
  private val hdcCurfewTimesRepository = mock<HdcCurfewTimesRepository>()
  private val licenceRepository = mock<LicenceRepository>()

  private val service =
    HdcService(
      hdcApiClient,
      hdcCurfewTimesRepository,
      licenceRepository,
    )

  @BeforeEach
  fun reset() {
    reset(licenceRepository)
    reset(hdcCurfewTimesRepository)
    reset(hdcApiClient)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully from hdcApiClient`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(emptyList())
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully from hdcCurfewTimesRepository`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(anEntitySetOfCurfewTimes)
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewTimes = emptyList(),
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there is no curfew address`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(emptyList())
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewAddress = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isNull()
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there are no first night curfew hours`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(emptyList())
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        firstNightCurfewHours = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isNull()
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully when there are no curfew times`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(emptyList<EntityHdcCurfewTimes>())
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewTimes = emptyList(),
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEmpty()
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns address details in HDC licence data successfully when the second line is not set`() {
    val anAddress = aCurfewAddress.copy(
      addressLine2 = null,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcCurfewTimesRepository.findByLicenceId(anyLong())).thenReturn(emptyList<EntityHdcCurfewTimes>())
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewAddress = anAddress,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(anAddress)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  private companion object {
    val aLicenceEntity = TestData.createHdcLicence()

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

    val anEntitySetOfCurfewTimes =
      listOf(
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          TestData.createHdcLicence(),
          7,
          DayOfWeek.SUNDAY,
          LocalTime.of(20, 0),
          DayOfWeek.MONDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
      )

    val aModelSetOfCurfewTimes =
      listOf(
        ModelHdcCurfewTimes(
          1L,
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          7,
          DayOfWeek.SUNDAY,
          LocalTime.of(20, 0),
          DayOfWeek.MONDAY,
          LocalTime.of(8, 0),
        ),
      )

    val someHdcLicenceData = HdcLicenceData(
      licenceId = 1L,
      aCurfewAddress,
      aSetOfFirstNightCurfewHours,
      aModelSetOfCurfewTimes,
    )
  }
}
