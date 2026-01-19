package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.EligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.SupportService
import java.time.LocalDate

class SupportServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val eligibilityService = mock<EligibilityService>()
  private val iS91DeterminationService = mock<IS91DeterminationService>()

  private val service = SupportService(
    prisonerSearchApiClient,
    eligibilityService,
    iS91DeterminationService,
  )

  @BeforeEach
  fun reset() {
    reset(
      prisonerSearchApiClient,
      eligibilityService,
      iS91DeterminationService,
    )
  }

  @Test
  fun `get ineligibility reasons for absent offender`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())

    val exception = assertThrows<IllegalStateException> {
      service.getIneligibilityReasons("A1234AA")
    }

    assertThat(exception.message).isEqualTo("Found 0 prisoners for: A1234AA")
  }

  @Test
  fun `get ineligibility reasons for present offender`() {
    val hdcPrisoner = aPrisonerSearchResult.copy(homeDetentionCurfewEligibilityDate = LocalDate.now())

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(hdcPrisoner))
    whenever(eligibilityService.getEligibilityAssessment(eq(hdcPrisoner))).thenReturn(
      EligibilityAssessment(
        genericIneligibilityReasons = listOf("A reason", "Approved for HDC"),
        crdIneligibilityReasons = emptyList(),
        prrdIneligibilityReasons = emptyList(),
        isEligible = false,
        eligibleKind = null,
      ),
    )

    val eligibilityAssessment = service.getIneligibilityReasons("A1234AA")
    assertThat(eligibilityAssessment.isEligible).isEqualTo(false)
    assertThat(eligibilityAssessment.genericIneligibilityReasons).containsExactly("A reason", "Approved for HDC")
    assertThat(eligibilityAssessment.crdIneligibilityReasons).isEmpty()
    assertThat(eligibilityAssessment.prrdIneligibilityReasons).isEmpty()
    assertThat(eligibilityAssessment.eligibleKind).isNull()
    assertThat(eligibilityAssessment.ineligibilityReasons).containsExactly("A reason", "Approved for HDC")
  }

  @Test
  fun `get is-91 status for absent offender`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(emptyList())

    val exception = assertThrows<IllegalStateException> {
      service.getIS91Status("A1234AA")
    }

    assertThat(exception.message).isEqualTo("Found 0 prisoners for: A1234AA")
  }

  @Test
  fun `get is-91 status for present offender returns true for an illegal immigrant offence code`() {
    val prisoner = aPrisonerSearchResult.copy(mostSeriousOffence = "ILLEGAL IMMIGRANT/DETAINEE")

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(prisoner))
    whenever(iS91DeterminationService.isIS91Case(prisoner)).thenReturn(true)

    val status = service.getIS91Status("A1234AA")
    assertThat(status).isTrue()
  }

  @Test
  fun `get is-91 status for present offender returns false for any other outcome code`() {
    val prisoner = aPrisonerSearchResult.copy(mostSeriousOffence = "OFFENCE1")

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(prisoner))
    whenever(iS91DeterminationService.isIS91Case(prisoner)).thenReturn(false)

    val status = service.getIS91Status("A1234AA")
    assertThat(status).isFalse()
  }

  private companion object {
    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "123456",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionExpiryDate = LocalDate.parse("2024-09-14"),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = LocalDate.parse("2023-09-14"),
      conditionalReleaseDate = LocalDate.parse("2023-09-14"),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
      recall = false,
      prisonId = "ABC",
      locationDescription = "HMP Moorland",
      bookNumber = "12345A",
      firstName = "Jane",
      middleNames = null,
      lastName = "Doe",
      dateOfBirth = LocalDate.parse("1985-01-01"),
      conditionalReleaseDateOverrideDate = null,
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
      topupSupervisionStartDate = null,
      croNumber = null,
    )
  }
}
