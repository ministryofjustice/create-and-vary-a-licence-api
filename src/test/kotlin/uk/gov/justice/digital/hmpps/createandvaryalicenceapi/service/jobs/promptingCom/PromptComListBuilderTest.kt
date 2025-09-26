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
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Case
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.HdcService.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.LicenceCreationService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.offenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.promptCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.ProbationCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.StaffEmail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class PromptComListBuilderTest {

  private val licenceRepository = mock<LicenceRepository>()
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val hdcService = mock<HdcService>()
  private val deliusApiClient = mock<DeliusApiClient>()
  private val licenceCreationService = mock<LicenceCreationService>()

  private val promptComListBuilder = PromptComListBuilder(
    licenceRepository,
    eligibilityService,
    releaseDateService,
    hdcService,
    deliusApiClient,
  )

  @BeforeEach
  fun reset() {
    reset(
      licenceRepository,
      eligibilityService,
      releaseDateService,
      hdcService,
      deliusApiClient,
      licenceCreationService,
    )

    whenever(licenceCreationService.determineLicenceKind(any(), any())).thenReturn(LicenceKind.CRD)
  }

  @Nested
  inner class ExcludeIneligibleCases {
    @Test
    fun eligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(eligibilityService.isEligibleForCvl(eq(prisoner), anyOrNull())).thenReturn(true)

      val result = promptComListBuilder.excludeIneligibleCases(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)
    }

    @Test
    fun ineligibleCase() {
      val prisoner = prisonerSearchResult()

      whenever(eligibilityService.isEligibleForCvl(eq(prisoner), anyOrNull())).thenReturn(false)

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

      whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(emptySet()))

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).containsExactly(prisoner)
    }

    @Test
    fun ineligibleCase() {
      val prisoner = prisonerSearchResult().copy(homeDetentionCurfewEligibilityDate = LocalDate.of(2022, 1, 2))

      whenever(hdcService.getHdcStatus(any())).thenReturn(HdcStatuses(setOf(prisoner.bookingId?.toLong()!!)))

      val result = promptComListBuilder.excludePrisonersWithHdc(listOf(prisoner))

      assertThat(result).isEmpty()
    }
  }

  @Nested
  inner class EnrichWithDeliusData {
    @Test
    fun presentCase() {
      val prisoner = prisonerSearchResult()

      whenever(deliusApiClient.getOffenderManagers(listOf(prisoner.prisonerNumber))).thenReturn(listOf(offenderManager()))

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
      val probationCase = ProbationCase(
        nomisId = prisoner.prisonerNumber,
        crn = "crn-1",
      )

      whenever(deliusApiClient.getProbationCases(listOf(prisoner.prisonerNumber))).thenReturn(listOf(probationCase))

      val result = promptComListBuilder.enrichWithDeliusData(listOf(prisoner))

      assertThat(result).isEmpty()
    }

    @Test
    fun missingCase() {
      val prisoner = prisonerSearchResult()

      whenever(deliusApiClient.getProbationCases(listOf(prisoner.prisonerNumber))).thenReturn(emptyList())

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
            email = "com@test.com",
          ),
        ),
      )

      val result = promptComListBuilder.enrichWithComEmail(listOf(promptCase))

      assertThat(result).containsExactly(promptCase to "com@test.com")
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

      val result = promptComListBuilder.enrichWithLicenceStartDates(listOf(promptCase to "com@test.com"))
      assertThat(result).containsExactly(
        promptCase to "com@test.com" to LocalDate.of(2022, 1, 2),
      )
    }

    @Test
    fun missingStartDate() {
      val promptCase = promptCase()

      whenever(
        releaseDateService.getLicenceStartDates(listOf(promptCase.prisoner)),
      ).thenReturn(emptyMap())

      val result = promptComListBuilder.enrichWithLicenceStartDates(listOf(promptCase to "com@test.com"))
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
      val promptCase = promptCase() to "user@test.com" to LocalDate.of(2022, 1, 10)
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
      val promptCase = promptCase() to "user@test.com" to LocalDate.of(2022, 1, 2)

      whenever(releaseDateService.isInHardStopPeriod(any(), anyOrNull())).thenReturn(false)

      val result = promptComListBuilder.excludeInHardStop(listOf(promptCase))

      assertThat(result).containsExactly(promptCase)
    }

    @Test
    fun inHardStopPeriod() {
      val promptCase = promptCase() to "user@test.com" to LocalDate.of(2022, 1, 2)

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
      ) to "user1@test.com" to LocalDate.of(2022, 1, 1)
      val promptCase2 = promptCase().copy(
        crn = "crn-2",
        prisoner = prisonerSearchResult().copy("A1234BB"),
        comStaffCode = "AAA",
      ) to "user1@test.com" to LocalDate.of(2022, 1, 2)
      val promptCase3 = promptCase().copy(
        crn = "crn-3",
        prisoner = prisonerSearchResult().copy("A1234CC"),
        comStaffCode = "BBB",
      ) to "user2@test.com" to LocalDate.of(2022, 1, 3)

      val result = promptComListBuilder.buildEmailsToSend(listOf(promptCase1, promptCase2, promptCase3))

      assertThat(result).isEqualTo(
        listOf(
          PromptComNotification(
            email = "user1@test.com",
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
            email = "user2@test.com",
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
