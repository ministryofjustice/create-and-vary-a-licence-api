package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind.PRRD
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EligibilityServiceTest {
  private var service = EligibilityService(clock, false)

  @Nested
  inner class CrdCases {
    @Test
    fun `Person is eligible for CVL`() {
      val result = service.getEligibilityAssessment(aPrisonerSearchResult, "")
      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(paroleEligibilityDate = LocalDate.now(clock).minusDays(1)),
        "",
      )
      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(paroleEligibilityDate = LocalDate.now(clock).plusYears(1)),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(legalStatus = "DEAD"),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("has died")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(indeterminateSentence = true),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is on indeterminate sentence")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(indeterminateSentence = null),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person does not have a conditional release date - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(conditionalReleaseDate = null),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the past - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = LocalDate.now(clock).minusDays(5),
          paroleEligibilityDate = LocalDate.now(clock).minusDays(10),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the future - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          confirmedReleaseDate = LocalDate.now(clock).plusDays(2),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on ineligible EDS - has a APD and a PED in the past - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED today - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock),
          actualParoleDate = LocalDate.now(clock),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED in the future - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(status = "INACTIVE TRN"),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(status = "INACTIVE OUT"),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is not active in prison")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person has a conditional release date (CRD) in the past - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(conditionalReleaseDate = LocalDate.now(clock).minusDays(1)),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("CRD in the past")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) before CRD - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(postRecallReleaseDate = LocalDate.now(clock).minusDays(1)),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) after CRD - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(postRecallReleaseDate = LocalDate.now(clock).plusDays(2)),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is a recall case")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on recall with a recall flag - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = true,
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date", "is a recall case")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Recall flag is null in NOMIS - not ineligibile due to recall`() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = null,
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person has no ARD and no CRD - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = null,
          conditionalReleaseDate = null,
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person with imprisonmentStatus ACTIVE IN - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(imprisonmentStatus = "ACTIVE IN"),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(imprisonmentStatus = "BOTUS"),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is breach of top up supervision case")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `returns bulk eligibility`() {
      val result = service.getEligibilityAssessments(
        listOf(aPrisonerSearchResult, aPrisonerSearchResult.copy(prisonerNumber = "A1234AB")),
        mapOf(
          "A1234AA" to "",
          "A1234AB" to "",
        ),
      )
      assertThat(result.size).isEqualTo(2)
      assertThat(result["A1234AA"]!!.isEligible).isTrue()
      assertThat(result["A1234AB"]!!.isEligible).isTrue()
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
      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, "")

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(PRRD)
    }

    @Test
    fun `Person is ineligible if PRRD is null`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = null,
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is ineligible if PRRD is in the past`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("post recall release date is in the past")
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is ineligible if PRRD is after LED`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
          licenceExpiryDate = LocalDate.now(clock),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("post recall release date is not before SLED")
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is ineligible if PRRD is after SED`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
          sentenceExpiryDate = LocalDate.now(clock),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("post recall release date is not before SLED")
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        ),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(PRRD)
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          legalStatus = "DEAD",
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("has died")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = true,
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is on indeterminate sentence")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = null,
        ),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(PRRD)
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE TRN",
        ),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(PRRD)
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE OUT",
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is not active in prison")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          imprisonmentStatus = "BOTUS",
        ),
        "",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is breach of top up supervision case")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `CRD case is eligible even if PRRD is null`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          conditionalReleaseDate = LocalDate.now(),
          postRecallReleaseDate = null,
        ),
        "",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
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
    fun `Eligible case is eligible if probation region and prison are permitted`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(prisonId = "PRISON_CODE"),
        "REGION_CODE_2",
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(PRRD)
    }

    @Test
    fun `Eligible case is ineligible if probation region is not permitted`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(prisonId = "PRISON_CODE"),
        "SOME_OTHER_REGION",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Eligible case is ineligible if prison code is not permitted`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(prisonId = "SOME_OTHER_PRISON"),
        "REGION_CODE_2",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(null)
    }

    @Test
    fun `Ineligible case is ineligible when probation region and prison are permitted`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = null,
          prisonId = "PRISON_CODE",
        ),
        "REGION_CODE_2",
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(null)
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

    fun nomisIdsToAreaCodes(regionCode: String = ""): Map<String, String> = mapOf(
      aPrisonerSearchResult.prisonerNumber to regionCode,
      aRecallPrisonerSearchResult.prisonerNumber to regionCode,
    )
  }
}
