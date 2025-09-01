package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EligibilityServiceTest {
  private val releaseDateService = mock<ReleaseDateService>()
  private var service = EligibilityService(clock, false)

  @Nested
  inner class CrdCases {
    @Test
    fun `Person is eligible for CVL`() {
      val result = service.isEligibleForCvl(aPrisonerSearchResult)
      assertThat(result).isTrue()
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val reasons = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        ),
      )
      assertThat(reasons).isEmpty()
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
        ),
      )
      assertThat(result).containsExactly("is eligible for parole")
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          legalStatus = "DEAD",
        ),
      )
      assertThat(result).containsExactly("has died")
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          indeterminateSentence = true,
        ),
      )
      assertThat(result).containsExactly("is on indeterminate sentence")
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val reasons = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          indeterminateSentence = null,
        ),
      )

      assertThat(reasons).isEmpty()
    }

    @Test
    fun `Person does not have a conditional release date - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
        ),
      )
      assertThat(result).containsExactly("has no conditional release date")
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the past - not eligible for CVL `() {
      whenever(releaseDateService.getLicenceStartDate(any(), anyOrNull())).thenReturn(LocalDate.now(clock).minusDays(5))
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = LocalDate.now(clock).minusDays(5),
          paroleEligibilityDate = LocalDate.now(clock).minusDays(10),
        ),
      )
      assertThat(result).containsExactly("is on non-eligible EDS")
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the future - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          confirmedReleaseDate = LocalDate.now(clock).plusDays(2),
        ),
      )
      assertThat(result).containsExactly("is on non-eligible EDS")
    }

    @Test
    fun `Person is on ineligible EDS - has a APD and a PED in the past - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
      )
      assertThat(result).containsExactly("is on non-eligible EDS")
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED today - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock),
          actualParoleDate = LocalDate.now(clock),
        ),
      )
      assertThat(result).containsExactly("is on non-eligible EDS")
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED in the future - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
      )
      assertThat(result).containsExactly("is eligible for parole", "is on non-eligible EDS")
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          status = "INACTIVE TRN",
        ),
      )
      assertThat(result).isEmpty()
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          status = "INACTIVE OUT",
        ),
      )
      assertThat(result).containsExactly("is not active in prison")
    }

    @Test
    fun `Person has a conditional release date (CRD) in the past - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = LocalDate.now(clock).minusDays(1),
        ),
      )
      assertThat(result).containsExactly("CRD in the past")
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) before CRD - eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
        ),
      )
      assertThat(result).isEmpty()
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) after CRD - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).plusDays(2),
        ),
      )
      assertThat(result).containsExactly("is a recall case")
    }

    @Test
    fun `Person is on recall with a recall flag - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = true,
        ),
      )
      assertThat(result).containsExactly("has no conditional release date", "is a recall case")
    }

    @Test
    fun `Recall flag is null in NOMIS - not ineligibile due to recall`() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = null,
        ),
      )
      assertThat(result).containsExactly("has no conditional release date")
    }

    @Test
    fun `Person has no ARD and no CRD - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = null,
          conditionalReleaseDate = null,
        ),
      )
      assertThat(result).containsExactly("has no conditional release date")
    }

    @Test
    fun `Person with imprisonmentStatus ACTIVE IN - eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          imprisonmentStatus = "ACTIVE IN",
        ),
      )
      assertThat(result).isEmpty()
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aPrisonerSearchResult.copy(
          imprisonmentStatus = "BOTUS",
        ),
      )
      assertThat(result).containsExactly("is breach of top up supervision case")
    }
  }

  @Nested
  inner class PrrdCases {
    @BeforeEach
    fun setup() {
      service = EligibilityService(clock, true)
    }

    @Test
    fun `Person is eligible for CVL`() {
      val result = service.isEligibleForCvl(aRecallPrisonerSearchResult)
      assertThat(result).isTrue()
    }

    @Test
    fun `Person is ineligible if PRRD is null`() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = null,
        ),
      )
      assertThat(result).containsExactly("has no conditional release date", "has no post recall release date")
    }

    @Test
    fun `Person is ineligible if PRRD is in the past`() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
        ),
      )
      assertThat(result).containsExactly("has no conditional release date", "post recall release date is in the past")
    }

    @Test
    fun `Person is ineligible if PRRD is after LED`() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
          licenceExpiryDate = LocalDate.now(clock),
        ),
      )
      assertThat(result).containsExactly(
        "has no conditional release date",
        "post recall release date is not before SLED",
      )
    }

    @Test
    fun `Person is ineligible if PRRD is after SED`() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
          sentenceExpiryDate = LocalDate.now(clock),
        ),
      )
      assertThat(result).containsExactly(
        "has no conditional release date",
        "post recall release date is not before SLED",
      )
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val reasons = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        ),
      )
      assertThat(reasons).isEmpty()
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
        ),
      )
      assertThat(result).containsExactly("is eligible for parole", "has no conditional release date")
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          legalStatus = "DEAD",
        ),
      )
      assertThat(result).containsExactly("has died", "has no conditional release date")
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = true,
        ),
      )
      assertThat(result).containsExactly("is on indeterminate sentence", "has no conditional release date")
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val reasons = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = null,
        ),
      )

      assertThat(reasons).isEmpty()
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE TRN",
        ),
      )
      assertThat(result).isEmpty()
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE OUT",
        ),
      )
      assertThat(result).containsExactly("is not active in prison", "has no conditional release date")
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getIneligibilityReasons(
        aRecallPrisonerSearchResult.copy(
          imprisonmentStatus = "BOTUS",
        ),
      )
      assertThat(result).containsExactly("is breach of top up supervision case", "has no conditional release date")
    }
  }

  @Nested
  inner class PrrdCasesFlagDisabled {
    val eligiblePrisonCodes = listOf("PRISON_CODE", "PRISON_CODE_2")
    val eligibleRegionCodes = listOf("REGION_CODE", "REGION_CODE_2")

    @BeforeEach
    fun setup() {
      service = EligibilityService(clock, false, eligiblePrisonCodes, eligibleRegionCodes)
    }

    @Test
    fun `Eligible case returns true if probation region and prison are permitted`() {
      val result = service.isEligibleForCvl(aRecallPrisonerSearchResult.copy(prisonId = "PRISON_CODE"), "REGION_CODE_2")
      assertThat(result).isTrue()
    }

    @Test
    fun `Eligible case returns false if probation region is not permitted`() {
      val result = service.isEligibleForCvl(aRecallPrisonerSearchResult.copy(prisonId = "PRISON_CODE"), "SOME_OTHER_REGION")
      assertThat(result).isFalse()
    }

    @Test
    fun `Eligible case returns false if prison code is not permitted`() {
      val result = service.isEligibleForCvl(aRecallPrisonerSearchResult.copy(prisonId = "SOME_OTHER_PRISON"), "REGION_CODE_2")
      assertThat(result).isFalse()
    }

    @Test
    fun `Ineligible case returns false when probation region and prison are permitted`() {
      val result = service.isEligibleForCvl(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = null,
          prisonId = "PRISON_CODE",
        ),
        "REGION_CODE_2",
      )
      assertThat(result).isFalse()
    }
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-11-03T00:00:00Z"), ZoneId.systemDefault())

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "54321",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.now(clock).plusYears(1),
      topupSupervisionExpiryDate = LocalDate.now(clock).plusYears(1),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.now(clock).plusDays(1),
      confirmedReleaseDate = LocalDate.now(clock).plusDays(1),
      conditionalReleaseDate = LocalDate.now(clock).plusDays(1),
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

    val aRecallPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "54321",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.now(clock).plusYears(1),
      topupSupervisionExpiryDate = LocalDate.now(clock).plusYears(1),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.now(clock).plusDays(1),
      confirmedReleaseDate = null,
      conditionalReleaseDate = null,
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
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
      sentenceExpiryDate = LocalDate.now(clock).plusYears(1),
      topupSupervisionStartDate = null,
      croNumber = null,
    )
  }
}
