package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.jobs.promptingCom

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.promptCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OffenderDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.OtherIds
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffEmail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class PromptComListBuilderTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val probationSearchApiClient = mock<ProbationSearchApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val prisonApiClient = mock<PrisonApiClient>()
  private val deliusApiClient = mock<DeliusApiClient>()

  private val promptComListBuilder = PromptComListBuilder(
    licenceRepository,
    probationSearchApiClient,
    eligibilityService,
    releaseDateService,
    prisonApiClient,
    deliusApiClient,
  )

  @BeforeEach
  fun reset() = reset(
    licenceRepository,
    probationSearchApiClient,
    eligibilityService,
    releaseDateService,
    prisonApiClient,
    deliusApiClient,
  )

  @Nested
  inner class ExcludeIneligibleCases {
    @Test
    fun eligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(eligibilityService.isEligibleForCvl(prisoner)).thenReturn(true)

      val result = promptComListBuilder.excludeIneligibleCases(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)
    }

    @Test
    fun ineligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(eligibilityService.isEligibleForCvl(prisoner)).thenReturn(false)

      val result = promptComListBuilder.excludeIneligibleCases(listOf(prisoner))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class ExcludeInflightLicences {
    @Test
    fun eligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(
        licenceRepository.findBookingIdsForLicencesInState(
          listOf(prisoner.prisonerNumber),
          LicenceStatus.IN_FLIGHT_LICENCES,
        ),
      ).thenReturn(emptySet())

      val result = promptComListBuilder.excludeInflightLicences(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)
    }

    @Test
    fun ineligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(
        licenceRepository.findBookingIdsForLicencesInState(
          listOf(prisoner.prisonerNumber),
          LicenceStatus.IN_FLIGHT_LICENCES,
        ),
      ).thenReturn(setOf(prisoner.bookingId!!.toLong()))

      val result = promptComListBuilder.excludeInflightLicences(listOf(prisoner))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class ExcludePrisonersWithHdc {
    @Test
    fun eligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(prisonApiClient.getHdcStatuses(listOf(prisoner.bookingId!!.toLong()))).thenReturn(emptyList())

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)
    }

    @Test
    fun ineligibleCase() {
      val prisoner = prisonerSearchResult().copy(homeDetentionCurfewEligibilityDate = LocalDate.of(2022, 1, 2))

      whenever(prisonApiClient.getHdcStatuses(listOf(prisoner.bookingId!!.toLong()))).thenReturn(
        listOf(
          hdcPrisonerStatus().copy(bookingId = prisoner.bookingId.toLong(), approvalStatus = "APPROVED"),
        ),
      )

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).isEmpty()
    }

    @Test
    fun eligibleDueToNotBeingApproved() {
      val prisoner = prisonerSearchResult().copy(homeDetentionCurfewEligibilityDate = LocalDate.of(2022, 1, 2))

      whenever(prisonApiClient.getHdcStatuses(listOf(prisoner.bookingId!!.toLong()))).thenReturn(
        listOf(
          hdcPrisonerStatus().copy(bookingId = prisoner.bookingId.toLong(), approvalStatus = ""),
        ),
      )

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)

      verify(prisonApiClient).getHdcStatuses(listOf(prisoner.bookingId.toLong()))
    }

    @Test
    fun eligibleDueToNoHDCED() {
      val prisoner = prisonerSearchResult().copy(homeDetentionCurfewEligibilityDate = null)

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)

      verify(prisonApiClient).getHdcStatuses(emptyList())
    }
  }

  @Nested
  inner class EnrichWithDeliusData {
    @Test
    fun presentCase() {
      val prisoner = prisonerSearchResult()
      val offenderDetail = OffenderDetail(
        otherIds = OtherIds(nomsNumber = prisoner.prisonerNumber, crn = "crn-1"),
        offenderId = 1L,
        offenderManagers = listOf(offenderManager()),
      )

      whenever(
        probationSearchApiClient.searchForPeopleByNomsNumber(listOf(prisoner.prisonerNumber)),
      ).thenReturn(listOf(offenderDetail))

      val result = promptComListBuilder.enrichWithDeliusData(listOf(prisoner))

      assertThat(result).containsExactly(
        PromptCase(
          prisoner = prisoner,
          crn = "crn-1",
          comStaffCode = "staff-code-1",
          comName = "forenames surname",
          comAllocationDate = LocalDate.of(2022, 1, 2),
        ),
      )
    }

    @Test
    fun presentCaseWithInactiveCom() {
      val prisoner = prisonerSearchResult()
      val offenderDetail = OffenderDetail(
        otherIds = OtherIds(nomsNumber = prisoner.prisonerNumber, crn = "crn-1"),
        offenderId = 1L,
        offenderManagers = listOf(offenderManager().copy(active = false)),
      )

      whenever(
        probationSearchApiClient.searchForPeopleByNomsNumber(listOf(prisoner.prisonerNumber)),
      ).thenReturn(listOf(offenderDetail))

      val result = promptComListBuilder.enrichWithDeliusData(listOf(prisoner))

      assertThat(result).isEmpty()
    }

    @Test
    fun missingCase() {
      val prisoner = prisonerSearchResult()

      whenever(
        probationSearchApiClient.searchForPeopleByNomsNumber(listOf(prisoner.prisonerNumber)),
      ).thenReturn(emptyList())

      val result = promptComListBuilder.enrichWithDeliusData(listOf(prisoner))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class EnrichWithComEmail {
    @Test
    fun presentCase() {
      val promptCase = promptCase()

      whenever(deliusApiClient.getStaffEmails(listOf(promptCase.crn))).thenReturn(
        listOf(
          StaffEmail(
            code = promptCase.comStaffCode,
            email = "com@user.com",
          ),
        ),
      )

      val result = promptComListBuilder.enrichWithComEmail(listOf(promptCase))

      assertThat(result).containsExactly(promptCase to "com@user.com")
    }

    @Test
    fun missingComEmail() {
      val promptCase = promptCase()

      whenever(deliusApiClient.getStaffEmails(listOf(any()))).thenReturn(emptyList())

      val result = promptComListBuilder.enrichWithComEmail(listOf(promptCase))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class EnrichWithLicenceStartDates {
    @Test
    fun present() {
      val promptCase = promptCase()

      whenever(
        releaseDateService.getLicenceStartDates(listOf(promptCase.prisoner)),
      ).thenReturn(
        mapOf(
          promptCase.prisoner.prisonerNumber to LocalDate.of(2022, 1, 2),
        ),
      )

      val result = promptComListBuilder.enrichWithLicenceStartDates(listOf(promptCase to "com@email.com"))
      assertThat(result).containsExactly(
        promptCase to "com@email.com" to LocalDate.of(2022, 1, 2),
      )
    }

    @Test
    fun missingStartDate() {
      val promptCase = promptCase()

      whenever(
        releaseDateService.getLicenceStartDates(listOf(promptCase.prisoner)),
      ).thenReturn(emptyMap())

      val result = promptComListBuilder.enrichWithLicenceStartDates(listOf(promptCase to "com@email.com"))
      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class ExcludeOutOfRangeDates {
    @ParameterizedTest
    @CsvSource(
      value = [
        "2022-01-08, 2022-01-09, true",
        "2022-01-09, 2022-01-10, false",
        "2022-01-10, 2022-01-11, false",
        "2022-01-11, 2022-01-12, true",
      ],
    )
    fun check(start: String, end: String, excluded: Boolean) {
      val promptCase = promptCase() to "user@email.com" to LocalDate.of(2022, 1, 10)
      val result = promptComListBuilder.excludeOutOfRangeDates(
        listOf(promptCase),
        LocalDate.parse(start),
        LocalDate.parse(end),
      )

      if (excluded) {
        assertThat(result).isEmpty()
      } else {
        assertThat(result).containsExactly(promptCase)
      }
    }
  }

  @Nested
  inner class ExcludeWithHardStop {
    @Test
    fun notInHardStop() {
      val promptCase = promptCase() to "user@email.com" to LocalDate.of(2022, 1, 2)

      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false)

      val result = promptComListBuilder.excludeInHardStop(listOf(promptCase))

      assertThat(result).containsExactly(promptCase)
    }

    @Test
    fun inHardStopPeriod() {
      val promptCase = promptCase() to "user@email.com" to LocalDate.of(2022, 1, 2)

      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(true)

      val result = promptComListBuilder.excludeInHardStop(listOf(promptCase))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class BuildEmailsToSend {
    @Test
    fun fewCases() {
      val promptCase1 = promptCase().copy(
        crn = "crn-1",
        prisoner = prisonerSearchResult().copy("A1234AA"),
        comStaffCode = "AAA",
      ) to "user1@email.com" to LocalDate.of(2022, 1, 1)
      val promptCase2 = promptCase().copy(
        crn = "crn-2",
        prisoner = prisonerSearchResult().copy("A1234BB"),
        comStaffCode = "AAA",
      ) to "user1@email.com" to LocalDate.of(2022, 1, 2)
      val promptCase3 = promptCase().copy(
        crn = "crn-3",
        prisoner = prisonerSearchResult().copy("A1234CC"),
        comStaffCode = "BBB",
      ) to "user2@email.com" to LocalDate.of(2022, 1, 3)

      val result = promptComListBuilder.buildEmailsToSend(listOf(promptCase1, promptCase2, promptCase3))

      assertThat(result).isEqualTo(
        listOf(
          PromptComNotification(
            email = "user1@email.com",
            comName = "John Doe",
            initialPromptCases = listOf(
              Case(
                crn = "crn-1",
                name = "A Prisoner",
                releaseDate = LocalDate.of(2022, 1, 1),
              ),
              Case(
                crn = "crn-2",
                name = "A Prisoner",
                releaseDate = LocalDate.of(2022, 1, 2),
              ),
            ),
          ),
          PromptComNotification(
            email = "user2@email.com",
            comName = "John Doe",
            initialPromptCases = listOf(
              Case(
                crn = "crn-3",
                name = "A Prisoner",
                releaseDate = LocalDate.of(2022, 1, 3),
              ),
            ),
          ),
        ),
      )
    }
  }
}
