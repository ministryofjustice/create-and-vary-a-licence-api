package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.assertj.core.groups.Tuple.tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateCurfewTimesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.currentPrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.FirstNight
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcLicenceData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.HdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.HDC
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Optional
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewAddress as EntityHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.HdcCurfewTimes as EntityHdcCurfewTimes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewAddress as ModelHdcCurfewAddress
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.HdcCurfewTimes as ModelHdcCurfewTimes

class HdcServiceTest {
  private val hdcApiClient = mock<HdcApiClient>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val licenceRepository = mock<LicenceRepository>()
  private val staffRepository = mock<StaffRepository>()
  private val auditService = mock<AuditService>()

  private val service =
    HdcService(
      hdcApiClient,
      prisonApiClient,
      licenceRepository,
      staffRepository,
      auditService,
    )

  @BeforeEach
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(
      licenceRepository,
      hdcApiClient,
      staffRepository,
    )
  }

  @Test
  fun `getHdcLicenceDataByBookingId returns HDC curfew times successfully`() {
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceDataByBookingId(54321)
    assertThat(result).isNotNull
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceDataByBookingId returns null if no curfew times present`() {
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(
      someHdcLicenceData.copy(
        curfewTimes = null,
      ),
    )
    val result = service.getHdcLicenceDataByBookingId(54321)
    assertThat(result?.curfewTimes).isNull()
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceDataByBookingId returns HDC curfew address successfully`() {
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceDataByBookingId(54321)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aModelCurfewAddress)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceDataByBookingId returns null if no curfew address present`() {
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(
      someHdcLicenceData.copy(
        curfewAddress = null,
      ),
    )
    val result = service.getHdcLicenceDataByBookingId(54321)
    assertThat(result?.curfewAddress).isNull()
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully from hdcApiClient`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
    whenever(hdcApiClient.getByBookingId(54321)).thenReturn(someHdcLicenceData)
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aModelCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(aModelSetOfCurfewTimes)
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns HDC licence data successfully from licenceRepository`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntityWithCurfewDetails))
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData.copy(
        curfewTimes = emptyList(),
        curfewAddress = null,
      ),
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result).isNotNull
    assertThat(result?.curfewAddress).isEqualTo(aModelCurfewAddress)
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
    assertThat(result?.curfewAddress).isEqualTo(aModelCurfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(aSetOfFirstNightCurfewHours)
    assertThat(result?.curfewTimes).isNull()
    verify(hdcApiClient, times(1)).getByBookingId(54321L)
  }

  @Test
  fun `getHdcLicenceData returns address details in HDC licence data successfully when the second line is not set`() {
    val anAddress = aModelCurfewAddress.copy(
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
  fun `getHdcLicenceData returns curfew information for HDC variations`() {
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(createHdcVariationLicence()))
    whenever(hdcApiClient.getByBookingId(54321L)).thenReturn(
      someHdcLicenceData,
    )
    val result = service.getHdcLicenceData(1)
    assertThat(result?.curfewAddress).isEqualTo(someHdcLicenceData.curfewAddress)
    assertThat(result?.firstNightCurfewHours).isEqualTo(someHdcLicenceData.firstNightCurfewHours)
    assertThat(result?.curfewTimes).isEqualTo(someHdcLicenceData.curfewTimes)
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
  }

  @Nested
  inner class EligibleForHdcLicenceTest {

    @Test
    fun `checkEligibleForHdcLicence does not throw error when all conditions are met`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(
        hdcPrisonerStatus().copy(
          approvalStatus = "APPROVED",
        ),
      )
      whenever(hdcApiClient.getByBookingId(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(someHdcLicenceData)
      assertDoesNotThrow {
        service.checkEligibleForHdcLicence(aPrisonerSearchResult, someHdcLicenceData)
      }
    }

    @Test
    fun `checkEligibleForHdcLicence throws error when HDCAD is missing`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = null,
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      val exception = assertThrows<IllegalStateException> {
        service.checkEligibleForHdcLicence(aPrisonerSearchResult, someHdcLicenceData)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as it is missing a HDCAD")
    }

    @Test
    fun `checkEligibleForHdcLicence throws error when HDCED is missing`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = null,
      )
      val exception = assertThrows<IllegalStateException> {
        service.checkEligibleForHdcLicence(aPrisonerSearchResult, someHdcLicenceData)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as it is missing a HDCED")
    }

    @Test
    fun `checkEligibleForHdcLicence throws error when not approved for HDC`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(hdcPrisonerStatus())
      val exception = assertThrows<IllegalStateException> {
        service.checkEligibleForHdcLicence(aPrisonerSearchResult, someHdcLicenceData)
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as they are not approved for HDC")
    }

    @Test
    fun `checkEligibleForHdcLicence throws error when there is no curfew address`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(
        hdcPrisonerStatus().copy(
          approvalStatus = "APPROVED",
        ),
      )
      whenever(hdcApiClient.getByBookingId(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(
        someHdcLicenceData.copy(
          curfewAddress = null,
        ),
      )
      val exception = assertThrows<IllegalStateException> {
        service.checkEligibleForHdcLicence(
          aPrisonerSearchResult,
          someHdcLicenceData.copy(
            curfewAddress = null,
          ),
        )
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as there is no curfew address")
    }

    @Test
    fun `checkEligibleForHdcLicence throws error when there are no curfew times`() {
      val aPrisonerSearchResult = aPrisonerSearchResult.copy(
        homeDetentionCurfewActualDate = LocalDate.now(),
        homeDetentionCurfewEligibilityDate = LocalDate.now(),
      )
      whenever(prisonApiClient.getHdcStatus(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(
        hdcPrisonerStatus().copy(
          approvalStatus = "APPROVED",
        ),
      )
      whenever(hdcApiClient.getByBookingId(aPrisonerSearchResult.bookingId!!.toLong())).thenReturn(
        someHdcLicenceData.copy(
          curfewAddress = null,
        ),
      )
      val exception = assertThrows<IllegalStateException> {
        service.checkEligibleForHdcLicence(
          aPrisonerSearchResult,
          someHdcLicenceData.copy(
            curfewTimes = null,
          ),
        )
      }
      assertThat(exception.message).isEqualTo("HDC licence for A1234AA could not be created as curfew times are missing")
    }
  }

  @Nested
  inner class `update curfew times` {
    @Test
    fun `update curfew times for a licence`() {
      whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(aLicenceEntity))
      whenever(staffRepository.findByUsernameIgnoreCase("tcom")).thenReturn(aCom)

      val curfewTimes = aUpdatedModelSetOfCurfewTimes

      service.updateCurfewTimes(
        1,
        UpdateCurfewTimesRequest(
          curfewTimes = curfewTimes,
        ),
      )

      val licenceCaptor = ArgumentCaptor.forClass(HdcLicence::class.java)

      verify(licenceRepository, times(1)).saveAndFlush(licenceCaptor.capture())
      verify(auditService, times(1)).recordAuditEventUpdateHdcCurfewTimes(any(), any(), any())

      assertThat(licenceCaptor.value)
        .extracting("updatedByUsername", "updatedBy")
        .isEqualTo(listOf(aCom.username, aCom))

      assertThat(licenceCaptor.value.curfewTimes)
        .extracting<Tuple> {
          tuple(
            it?.fromDay,
            it?.fromTime,
            it?.untilDay,
            it?.untilTime,
          )
        }
        .contains(
          tuple(MONDAY, LocalTime.of(21, 0), TUESDAY, LocalTime.of(9, 0)),
          tuple(TUESDAY, LocalTime.of(21, 0), WEDNESDAY, LocalTime.of(9, 0)),
          tuple(WEDNESDAY, LocalTime.of(21, 0), THURSDAY, LocalTime.of(9, 0)),
          tuple(THURSDAY, LocalTime.of(21, 0), FRIDAY, LocalTime.of(9, 0)),
          tuple(FRIDAY, LocalTime.of(21, 0), SATURDAY, LocalTime.of(9, 0)),
          tuple(SATURDAY, LocalTime.of(21, 0), SUNDAY, LocalTime.of(9, 0)),
          tuple(SUNDAY, LocalTime.of(21, 0), MONDAY, LocalTime.of(9, 0)),
        )
    }
  }

  @Nested
  inner class `using Current HDC Status endpoint` {
    private val service =
      HdcService(
        hdcApiClient,
        prisonApiClient,
        licenceRepository,
        staffRepository,
        auditService,
        useCurrentHdcStatus = true,
      )

    @Test
    fun `getHdcStatuses sets approvedIds to those that are potential HDC releases`() {
      whenever(prisonApiClient.getCurrentHdcStatuses(listOf(1L, 3L, 4L, 5L))).thenReturn(
        listOf(
          currentPrisonerHdcStatus(bookingId = 1L, currentHdcStatus = HdcStatus.APPROVED),
          // Didn't request 2L as missing HDCED
          // Missing data for 3L
          currentPrisonerHdcStatus(bookingId = 4L, currentHdcStatus = HdcStatus.NOT_A_HDC_RELEASE),
          currentPrisonerHdcStatus(bookingId = 5L, currentHdcStatus = HdcStatus.ELIGIBILITY_CHECKS_COMPLETE),
        ),
      )

      val details = listOf(
        mapOf("bookingId" to 1L, "hdced" to LocalDate.now()),
        mapOf("bookingId" to 2L, "hdced" to null),
        mapOf("bookingId" to 3L, "hdced" to LocalDate.now()),
        mapOf("bookingId" to 4L, "hdced" to LocalDate.now()),
        mapOf("bookingId" to 5L, "hdced" to LocalDate.now()),
      )

      val result = service.getHdcStatus(details, { it["bookingId"] as Long }, { it["hdced"] as LocalDate? })

      assertThat(result).isNotNull
      assertThat(result.approvedIds).containsExactly(1L, 5L)
    }
  }

  private companion object {
    val aLicenceEntity = createHdcLicence()

    val aCom = communityOffenderManager()

    val anEntityCurfewAddress = EntityHdcCurfewAddress(
      1L,
      aLicenceEntity,
      "1 Test Street",
      "Test Area",
      "Test Town",
      null,
      "AB1 2CD",
    )

    val aModelCurfewAddress = ModelHdcCurfewAddress(
      1L,
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
          aLicenceEntity,
          1,
          MONDAY,
          LocalTime.of(20, 0),
          TUESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          2,
          TUESDAY,
          LocalTime.of(20, 0),
          WEDNESDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          3,
          WEDNESDAY,
          LocalTime.of(20, 0),
          THURSDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          4,
          THURSDAY,
          LocalTime.of(20, 0),
          FRIDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          5,
          FRIDAY,
          LocalTime.of(20, 0),
          SATURDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          6,
          SATURDAY,
          LocalTime.of(20, 0),
          SUNDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
        EntityHdcCurfewTimes(
          1L,
          aLicenceEntity,
          7,
          SUNDAY,
          LocalTime.of(20, 0),
          MONDAY,
          LocalTime.of(8, 0),
          LocalDateTime.of(2024, 8, 14, 9, 0),
        ),
      )

    val aModelSetOfCurfewTimes =
      listOf(
        ModelHdcCurfewTimes(
          1L,
          1,
          MONDAY,
          LocalTime.of(20, 0),
          TUESDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          2,
          TUESDAY,
          LocalTime.of(20, 0),
          WEDNESDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          3,
          WEDNESDAY,
          LocalTime.of(20, 0),
          THURSDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          4,
          THURSDAY,
          LocalTime.of(20, 0),
          FRIDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          5,
          FRIDAY,
          LocalTime.of(20, 0),
          SATURDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          6,
          SATURDAY,
          LocalTime.of(20, 0),
          SUNDAY,
          LocalTime.of(8, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          7,
          SUNDAY,
          LocalTime.of(20, 0),
          MONDAY,
          LocalTime.of(8, 0),
        ),
      )

    val aUpdatedModelSetOfCurfewTimes =
      listOf(
        ModelHdcCurfewTimes(
          1L,
          1,
          MONDAY,
          LocalTime.of(21, 0),
          TUESDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          2,
          TUESDAY,
          LocalTime.of(21, 0),
          WEDNESDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          3,
          WEDNESDAY,
          LocalTime.of(21, 0),
          THURSDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          4,
          THURSDAY,
          LocalTime.of(21, 0),
          FRIDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          5,
          FRIDAY,
          LocalTime.of(21, 0),
          SATURDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          6,
          SATURDAY,
          LocalTime.of(21, 0),
          SUNDAY,
          LocalTime.of(9, 0),
        ),
        ModelHdcCurfewTimes(
          1L,
          7,
          SUNDAY,
          LocalTime.of(21, 0),
          MONDAY,
          LocalTime.of(9, 0),
        ),
      )

    val aLicenceEntityWithCurfewDetails = createHdcLicence()
      .copy(
        curfewTimes = anEntitySetOfCurfewTimes,
        curfewAddress = anEntityCurfewAddress,
      )

    val someHdcLicenceData = HdcLicenceData(
      licenceId = 1L,
      aModelCurfewAddress,
      aSetOfFirstNightCurfewHours,
      aModelSetOfCurfewTimes,
    )

    val aPrisonerSearchResult = prisonerSearchResult()
  }
}
