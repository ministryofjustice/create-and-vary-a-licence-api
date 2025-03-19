package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.CurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes as EntityHdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes as ModelHdcCurfewTimes

class HdcServiceTest {
  private val hdcApiClient = mock<HdcApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val licenceRepository = mock<LicenceRepository>()

  private val service =
    HdcService(
      hdcApiClient,
      prisonApiClient,
      licenceRepository,
    )

  @BeforeEach
  fun reset() {
    reset(licenceRepository)
    reset(hdcApiClient)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully from hdcApiClient`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data with curfew times successfully from licenceRepository`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntityWithCurfewTimes))
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
  fun `getHdcLicenceData returns HDC licence data successfully when no recorded curfew times`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewTimes = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isNull()
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns address details in HDC licence data successfully when the second line is not set`() {
    val anAddress = aCurfewAddress.copy(
      addressLine2 = null,
    )
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
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

  @Test
  fun `getHdcLicenceData returns default first night hours if not provided from HDC`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        firstNightCurfewHours = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result?.firstNightCurfewHours).isEqualTo(HdcService.DEFAULT_FIRST_NIGHT_HOURS)
  }

  @Test
  fun getHdcStatuses() {
    whenever(prisonApiClient.getHdcStatuses(listOf(1L, 3L, 4L))).thenReturn(
      listOf(
        hdcPrisonerStatus().copy(bookingId = 1L, approvalStatus = "APPROVED"),
        // Didn't request 2L as missing HDCED
        // Missing data for 3L
        hdcPrisonerStatus().copy(bookingId = 4L, approvalStatus = "REJECTED"),
      ),
    )

    val details = listOf(
      mapOf("bookingId" to 1L, "hdced" to LocalDate.now()),
      mapOf("bookingId" to 2L, "hdced" to null),
      mapOf("bookingId" to 3L, "hdced" to LocalDate.now()),
      mapOf("bookingId" to 4L, "hdced" to LocalDate.now()),
    )

    val result = service.getHdcStatus(details, { it["bookingId"] as Long }, { it["hdced"] as LocalDate? })

    assertThat(result).isNotNull
    assertThat(result.approvedIds).containsExactly(1L)
  }

  @Test
  fun `isApprovedForHdc when approved`() {
    whenever(prisonApiClient.getHdcStatus(1L)).thenReturn(
      hdcPrisonerStatus().copy(bookingId = 1L, approvalStatus = "APPROVED"),
    )

    assertThat(service.isApprovedForHdc(1L, LocalDate.now())).isTrue
    assertThat(service.isApprovedForHdc(1L, null)).isFalse
  }

  @Test
  fun `isApprovedForHdc when not approved`() {
    whenever(prisonApiClient.getHdcStatus(2L)).thenReturn(
      hdcPrisonerStatus().copy(bookingId = 2L, approvalStatus = "NOT_APPROVED"),
    )

    assertThat(service.isApprovedForHdc(2L, LocalDate.now())).isFalse
    assertThat(service.isApprovedForHdc(2L, null)).isFalse
  }

  @Nested
  inner class HdcStatusesTest {
    val statuses = HdcStatuses(setOf(1L))

    @Test
    fun isWaitingForActivation() {
      assertThat(statuses.isWaitingForActivation(HDC, 1L)).isFalse
      assertThat(statuses.isWaitingForActivation(HDC, 2L)).isTrue

      assertThat(statuses.isWaitingForActivation(CRD, 1L)).isFalse
      assertThat(statuses.isWaitingForActivation(CRD, 2L)).isFalse
    }

    @Test
    fun canBeActivated() {
      assertThat(statuses.canBeActivated(HDC, 1L)).isTrue
      assertThat(statuses.canBeActivated(HDC, 2L)).isFalse

      assertThat(statuses.canBeActivated(CRD, 1L)).isFalse
      assertThat(statuses.canBeActivated(CRD, 2L)).isTrue
    }

    @Test
    fun isApproved() {
      assertThat(statuses.isApprovedForHdc(1L)).isTrue
      assertThat(statuses.isApprovedForHdc(2L)).isFalse
    }

    @Test
    fun canBeSeenByCom() {
      assertThat(statuses.canBeSeenByCom(HDC, 1L)).isTrue
      assertThat(statuses.canBeSeenByCom(HDC, 2L)).isFalse

      assertThat(statuses.canBeSeenByCom(CRD, 1L)).isFalse
      assertThat(statuses.canBeSeenByCom(CRD, 2L)).isTrue

      assertThat(statuses.canBeSeenByCom(null, 1L)).isFalse
      assertThat(statuses.canBeSeenByCom(null, 2L)).isTrue
    }

    @Test
    fun canUnstartedCaseBeSeenByCa() {
      assertThat(statuses.canUnstartedCaseBeSeenByCa(1L)).isFalse
      assertThat(statuses.canUnstartedCaseBeSeenByCa(2L)).isTrue
    }
  }

  @Nested
  inner class EligibleForHdcLicenceTest {

    @Test
    fun `isEligibleForHdcLicence returns true when all conditions are met`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(hdcPrisonerStatus().copy(approvalStatus = "APPROVED"))
      whenever(hdcApiClient.getByBookingId(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(someHdcLicenceData)
      val result = service.isEligibleForHdcLicence(aPrisonerSearchResult)
      assertThat(result).isTrue()
    }

    @Test
    fun `isEligibleForHdcLicence throws error when HDCAD is missing`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = null,
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      val exception = assertThrows<IllegalStateException> {
        service.isEligibleForHdcLicence(aPrisonerSearchResult)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as it is missing a HDCAD")
    }

    @Test
    fun `isEligibleForHdcLicence throws error when HDCED is missing`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = null,
      )
      val exception = assertThrows<IllegalStateException> {
        service.isEligibleForHdcLicence(aPrisonerSearchResult)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as it is missing a HDCED")
    }

    @Test
    fun `isEligibleForHdcLicence throws error when not approved for HDC`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(hdcPrisonerStatus())
      val exception = assertThrows<IllegalStateException> {
        service.isEligibleForHdcLicence(aPrisonerSearchResult)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as they are not approved for HDC")
    }

    @Test
    fun `isEligibleForHdcLicence throws error when there is no curfew address`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(hdcPrisonerStatus().copy(approvalStatus = "APPROVED"))
      whenever(hdcApiClient.getByBookingId(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(someHdcLicenceData.copy(curfewAddress = null))
      val exception = assertThrows<IllegalStateException> {
        service.isEligibleForHdcLicence(aPrisonerSearchResult)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as there is no curfew address")
    }
  }

  private companion object {
    val aLicenceEntity = createHdcLicence()

    val aCurfewAddress = CurfewAddress(
      "1 Test Street",
      "Test Area",
      "Test Town",
      null,
      "AB1 2CD",
    )

    val aSetOfFirstNightCurfewHours = FirstNight(
      LocalTime.of(16, 0),
      LocalTime.of(8, 0),
    )

    val anEntitySetOfCurfewTimes =
      listOf(
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          1,
          DayOfWeek.MONDAY,
          LocalTime.of(20, 0),
          DayOfWeek.TUESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          2,
          DayOfWeek.TUESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.WEDNESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          3,
          DayOfWeek.WEDNESDAY,
          LocalTime.of(20, 0),
          DayOfWeek.THURSDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          4,
          DayOfWeek.THURSDAY,
          LocalTime.of(20, 0),
          DayOfWeek.FRIDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          5,
          DayOfWeek.FRIDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SATURDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
          6,
          DayOfWeek.SATURDAY,
          LocalTime.of(20, 0),
          DayOfWeek.SUNDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          createHdcLicence(),
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

    val aLicenceEntityWithCurfewTimes = createHdcLicence()
      .copy(curfewTimes = anEntitySetOfCurfewTimes)

    val someHdcLicenceData = HdcLicenceData(
      licenceId = 1L,
      aCurfewAddress,
      aSetOfFirstNightCurfewHours,
      aModelSetOfCurfewTimes,
    )

    val aPrisonerSearchResult = prisonerSearchResult()
  }
}
