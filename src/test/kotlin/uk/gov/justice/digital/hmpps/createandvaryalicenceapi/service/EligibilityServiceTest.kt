package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aSentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind.CRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind.FIXED_TERM
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind.HDC
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EligibilityServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val hdcService = mock<HdcService>()
  private var service = EligibilityService(prisonApiClient, releaseDateService, clock)

  @Nested
  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  inner class PssProgressionRepeal {

    @ParameterizedTest
    @MethodSource("pssRepealCases")
    fun `pss repeal eligibility scenarios`(
      licenceExpiryDate: LocalDate?,
      topUpSupervisionExpiryDate: LocalDate?,
      expectedEligible: Boolean,
    ) {
      // Given
      val prisonerSearchResult = aPrisonerSearchResult.copy(
        licenceExpiryDate = licenceExpiryDate,
        topupSupervisionExpiryDate = topUpSupervisionExpiryDate,
      )

      // When
      val result = service.getEligibilityAssessment(prisonerSearchResult, HdcStatuses(emptyList()))

      // Then
      assertThat(result.isEligible).isEqualTo(expectedEligible)

      if (!expectedEligible) {
        assertThat(result.genericIneligibilityReasons)
          .containsExactly("PSS licences no longer supported")
      }
    }

    fun pssRepealCases(): List<Arguments> {
      val now = LocalDate.now(clock)

      return listOf(
        // repeal date passed / PSS blocked
        Arguments.of(null, now.plusDays(1), false),
        Arguments.of(now.plusDays(1), now.plusDays(1), true),
        Arguments.of(now.plusDays(1), null, true),
        Arguments.of(null, null, true),
      )
    }
  }

  @Nested
  inner class CrdCases {
    @Test
    fun `Person is eligible for CVL`() {
      val result = service.getEligibilityAssessment(aPrisonerSearchResult, HdcStatuses(emptyList()))
      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.hdcIneligibilityReasons).containsExactly("HDC licences not currently supported in CVL")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(paroleEligibilityDate = LocalDate.now(clock).minusDays(1)),
        HdcStatuses(emptyList()),
      )
      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(paroleEligibilityDate = LocalDate.now(clock).plusYears(1)),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(legalStatus = "DEAD"),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("has died")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(indeterminateSentence = true),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is on indeterminate sentence")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(indeterminateSentence = null),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person does not have a conditional release date - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(conditionalReleaseDate = null),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the past - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = LocalDate.now(clock).minusDays(5),
          paroleEligibilityDate = LocalDate.now(clock).minusDays(10),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on ineligible EDS - ARD is outside threshold in the future - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          confirmedReleaseDate = LocalDate.now(clock).plusDays(2),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on ineligible EDS - has a APD and a PED in the past - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED today - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock),
          actualParoleDate = LocalDate.now(clock),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on ineligible EDS - has a APD with a PED in the future - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(status = "INACTIVE TRN"),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(status = "INACTIVE OUT"),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("does not have eligible prison status")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person has a conditional release date (CRD) in the past not equal to sentence start date - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(conditionalReleaseDate = LocalDate.now(clock).minusDays(1)),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("CRD in the past and not eligible for time served")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) before CRD - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(postRecallReleaseDate = LocalDate.now(clock).minusDays(1)),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("post recall release date is in the past")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is on recall with a post recall release date (PRRD) after CRD - standard recall now eligible CVL `() {
      whenever(prisonApiClient.getSentenceAndRecallTypes(any(), any())).thenReturn(
        listOf(
          BookingSentenceAndRecallTypes(
            bookingId = aPrisonerSearchResult.bookingId!!.toLong(),
            listOf(
              SentenceAndRecallType(
                "type",
                SentenceRecallType(recallName = "standard", isStandardRecall = true, isFixedTermRecall = false),
              ),
            ),
          ),
        ),
      )
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(postRecallReleaseDate = LocalDate.now(clock).plusDays(2)),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is a recall case")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(EligibleKind.STANDARD)
    }

    @Test
    fun `Person is on recall with a recall flag - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = true,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date", "is a recall case")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Recall flag is null in NOMIS - not ineligible due to recall`() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = null,
          recall = null,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person has no ARD and no CRD - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          confirmedReleaseDate = null,
          conditionalReleaseDate = null,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person with imprisonmentStatus ACTIVE IN - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(imprisonmentStatus = "ACTIVE IN"),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(imprisonmentStatus = "BOTUS"),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is breach of top up supervision case")
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person with HDC approval - not eligible for CVL`() {
      val hdcStatuses = HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(
            bookingId = aPrisonerSearchResult.bookingId!!.toLong(),
            approvalStatus = "APPROVED",
          ),
        ),
      )
      val result = service.getEligibilityAssessment(aPrisonerSearchResult, hdcStatuses)

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is expected to be released on HDC")
      assertThat(result.prrdIneligibilityReasons).containsExactly(
        "has no post recall release date",
        "is expected to be released on HDC",
      )
      assertThat(result.hdcIneligibilityReasons).containsExactly("HDC licences not currently supported in CVL")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `returns bulk eligibility`() {
      val result = service.getEligibilityAssessments(
        listOf(aPrisonerSearchResult, aPrisonerSearchResult.copy(prisonerNumber = "A1234AB")),
        HdcStatuses(emptyList()),
      )
      assertThat(result.size).isEqualTo(2)
      assertThat(result["A1234AA"]!!.isEligible).isTrue()
      assertThat(result["A1234AB"]!!.isEligible).isTrue()
    }

    @Test
    fun `conditional release date and sentence start date of yesterday so eligible for a time served licence`() {
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = LocalDate.now(clock),
          sentenceStartDate = LocalDate.now(clock),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `conditional release date and sentence start date too far in the past no not eligible for a time served licence`() {
      val ssd = LocalDate.now(clock).minusDays(20)
      val result = service.getEligibilityAssessment(
        aPrisonerSearchResult.copy(
          conditionalReleaseDate = ssd,
          sentenceStartDate = ssd,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("CRD in the past and not eligible for time served")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person does not have an active booking - not eligible for CVL`() {
      val result = service.getEligibilityAssessments(
        listOf(
          aPrisonerSearchResult.copy(bookingId = null),
          aPrisonerSearchResult.copy(prisonerNumber = "A1234AB"),
        ),
        HdcStatuses(emptyList()),
      )
      assertThat(result["A1234AA"]!!.isEligible).isFalse()
      assertThat(result["A1234AA"]!!.genericIneligibilityReasons).containsExactly("no active booking")
      assertThat(result["A1234AA"]!!.crdIneligibilityReasons).isEmpty()
      assertThat(result["A1234AA"]!!.prrdIneligibilityReasons).isEmpty()
      assertThat(result["A1234AA"]!!.eligibleKind).isNull()

      assertThat(result["A1234AB"]!!.isEligible).isTrue()
      assertThat(result["A1234AB"]!!.genericIneligibilityReasons).isEmpty()
      assertThat(result["A1234AB"]!!.crdIneligibilityReasons).isEmpty()
      assertThat(result["A1234AB"]!!.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result["A1234AB"]!!.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is a restricted patient - eligible for CVL`() {
      val prisoner = aPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
        restrictedPatient = true,
        supportingPrisonId = "MDI",
      )
      val service = EligibilityService(
        prisonApiClient,
        releaseDateService,
        clock,
        restrictedPatientsEnabled = true,
      )

      val result = service.getEligibilityAssessment(prisoner, HdcStatuses(emptyList()))

      println(result)

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Person is a restricted patient but toggle is off -  not eligible for CVL`() {
      val prisoner = aPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
        restrictedPatient = true,
        supportingPrisonId = "MDI",
      )
      val service = EligibilityService(
        prisonApiClient,
        releaseDateService,
        clock,
        restrictedPatientsEnabled = false,
      )

      val result = service.getEligibilityAssessment(prisoner, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).contains("does not have eligible prison status")
    }
  }

  @Nested
  inner class PrrdCases {
    @BeforeEach
    fun setup() {
      service = EligibilityService(prisonApiClient, releaseDateService, clock)

      whenever(prisonApiClient.getSentenceAndRecallTypes(any(), anyOrNull())).thenReturn(
        listOf(
          BookingSentenceAndRecallTypes(
            bookingId = 123456,
            sentenceTypeRecallTypes = listOf(aSentenceAndRecallType()),
          ),
        ),
      )
      whenever(releaseDateService.calculatePrrdLicenceStartDate(any())).thenReturn(aRecallPrisonerSearchResult.licenceExpiryDate)
      whenever(releaseDateService.isReleaseAtLed(any(), any())).thenReturn(false)
    }

    @Test
    fun `Person is eligible for CVL`() {
      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.hdcIneligibilityReasons).containsExactly("HDC licences not currently supported in CVL")
      assertThat(result.eligibleKind).isEqualTo(FIXED_TERM)
    }

    @Test
    fun `Person is ineligible if PRRD is null`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = null,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is ineligible if PRRD is in the past`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("post recall release date is in the past")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(FIXED_TERM)
    }

    @Test
    fun `Person is parole eligible - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is eligible for parole")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person has an incorrect legal status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          legalStatus = "DEAD",
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("has died")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is on an indeterminate sentence - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = true,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is on indeterminate sentence")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `isIndeterminateSentence is null - eligible for CVL`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          indeterminateSentence = null,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(FIXED_TERM)
    }

    @Test
    fun `Person is an inactive transfer - eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE TRN",
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(FIXED_TERM)
    }

    @Test
    fun `Person does not have an active prison status - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          status = "INACTIVE OUT",
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("does not have eligible prison status")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person with imprisonmentStatus BOTUS - not eligible for CVL `() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          imprisonmentStatus = "BOTUS",
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).containsExactly("is breach of top up supervision case")
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `CRD case is eligible even if PRRD is null`() {
      val result = service.getEligibilityAssessment(
        aRecallPrisonerSearchResult.copy(
          conditionalReleaseDate = LocalDate.now(),
          postRecallReleaseDate = null,
        ),
        HdcStatuses(emptyList()),
      )

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).isEmpty()
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.eligibleKind).isEqualTo(CRD)
    }

    @Test
    fun `Case with a standard recall has the correct EligibleKind`() {
      whenever(prisonApiClient.getSentenceAndRecallTypes(any(), anyOrNull())).thenReturn(
        listOf(
          BookingSentenceAndRecallTypes(
            bookingId = 123456,
            sentenceTypeRecallTypes = listOf(
              aSentenceAndRecallType(),
              aSentenceAndRecallType(
                sentenceRecallType = aRecallType(
                  isStandardRecall = true,
                  isFixedTermRecall = false,
                ),
              ),
            ),
          ),
        ),
      )

      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(EligibleKind.STANDARD)
    }

    @Test
    fun `Case with no recall sentence is ineligible`() {
      whenever(prisonApiClient.getSentenceAndRecallTypes(any(), anyOrNull())).thenReturn(
        listOf(
          BookingSentenceAndRecallTypes(
            bookingId = 123456,
            sentenceTypeRecallTypes = listOf(
              aSentenceAndRecallType(
                sentenceRecallType = aRecallType(
                  isStandardRecall = false,
                  isFixedTermRecall = false,
                ),
              ),
            ),
          ),
        ),
      )

      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("is on an unidentified non-fixed term recall")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Case with no active sentences is ineligible`() {
      whenever(prisonApiClient.getSentenceAndRecallTypes(any(), anyOrNull())).thenReturn(
        emptyList(),
      )

      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("does not have any active sentences")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person who would be on an AP licence being released at SLED - not eligible for CVL`() {
      whenever(releaseDateService.isReleaseAtLed(any(), any())).thenReturn(true)
      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).containsExactly("is being released at SLED")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person with HDC approval - not eligible for CVL`() {
      val hdcStatuses = HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(
            bookingId = aRecallPrisonerSearchResult.bookingId!!.toLong(),
            approvalStatus = "APPROVED",
          ),
        ),
      )
      val result = service.getEligibilityAssessment(aRecallPrisonerSearchResult, hdcStatuses)

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly(
        "has no conditional release date",
        "is expected to be released on HDC",
      )
      assertThat(result.prrdIneligibilityReasons).containsExactly("is expected to be released on HDC")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Person is a restricted patient - eligible for CVL`() {
      val prisoner = aRecallPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
        restrictedPatient = true,
        supportingPrisonId = "MDI",
      )
      val service = EligibilityService(
        prisonApiClient,
        releaseDateService,
        clock,
        restrictedPatientsEnabled = true,
      )

      val result = service.getEligibilityAssessment(prisoner, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.prrdIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(FIXED_TERM)
    }

    @Test
    fun `Person is a restricted patient but toggle is off -  not eligible for CVL`() {
      val prisoner = aRecallPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
        restrictedPatient = true,
        supportingPrisonId = "MDI",
      )
      val service = EligibilityService(
        prisonApiClient,
        releaseDateService,
        clock,
        restrictedPatientsEnabled = false,
      )

      val result = service.getEligibilityAssessment(prisoner, HdcStatuses(emptyList()))

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).contains("does not have eligible prison status")
    }
  }

  @Nested
  inner class HdcCases {
    private var service =
      EligibilityService(prisonApiClient, releaseDateService, clock, hdcEnabled = true)
    val hdcStatuses = HdcStatuses(
      listOf(
        hdcPrisonerStatus().copy(
          bookingId = anHdcPrisonerSearchResult.bookingId!!.toLong(),
          approvalStatus = "APPROVED",
        ),
      ),
    )

    @BeforeEach
    fun reset() {
      whenever(hdcService.getHdcStatus(any())).thenReturn(
        hdcStatuses,
      )
    }

    @Test
    fun `person is eligible for CVL`() {
      val result = service.getEligibilityAssessment(anHdcPrisonerSearchResult, hdcStatuses)

      assertThat(result.isEligible).isTrue()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is expected to be released on HDC")
      assertThat(result.prrdIneligibilityReasons).containsExactly(
        "has no post recall release date",
        "is expected to be released on HDC",
      )
      assertThat(result.hdcIneligibilityReasons).isEmpty()
      assertThat(result.eligibleKind).isEqualTo(HDC)
    }

    @Test
    fun `CRD is missing - ineligible for CVL`() {
      val result =
        service.getEligibilityAssessment(anHdcPrisonerSearchResult.copy(conditionalReleaseDate = null), hdcStatuses)

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly(
        "has no conditional release date",
        "is expected to be released on HDC",
      )
      assertThat(result.prrdIneligibilityReasons).containsExactly(
        "has no post recall release date",
        "is expected to be released on HDC",
      )
      assertThat(result.hdcIneligibilityReasons).containsExactly("has no conditional release date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `HDCAD is missing - ineligible for CVL`() {
      val result = service.getEligibilityAssessment(
        anHdcPrisonerSearchResult.copy(homeDetentionCurfewActualDate = null),
        hdcStatuses,
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is expected to be released on HDC")
      assertThat(result.prrdIneligibilityReasons).containsExactly(
        "has no post recall release date",
        "is expected to be released on HDC",
      )
      assertThat(result.hdcIneligibilityReasons).containsExactly("has no home detention curfew actual date")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `CRD is under 10 days in the future - ineligible for CVL`() {
      val result = service.getEligibilityAssessment(
        anHdcPrisonerSearchResult.copy(
          conditionalReleaseDate = LocalDate.now(clock).plusDays(9),
        ),
        hdcStatuses,
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is expected to be released on HDC")
      assertThat(result.prrdIneligibilityReasons).containsExactly(
        "has no post recall release date",
        "is expected to be released on HDC",
      )
      assertThat(result.hdcIneligibilityReasons).containsExactly("has CRD fewer than 10 days in the future")
      assertThat(result.eligibleKind).isNull()
    }

    @Test
    fun `Case does not have HDC approval - ineligible for CVL`() {
      val rejectedHdcStatuses = HdcStatuses(
        listOf(
          hdcPrisonerStatus().copy(
            bookingId = anHdcPrisonerSearchResult.bookingId!!.toLong(),
            approvalStatus = "REJECTED",
          ),
        ),
      )
      whenever(hdcService.getHdcStatus(any())).thenReturn(
        rejectedHdcStatuses,
      )

      // Also make the case ineligible for a CRD licence
      val result = service.getEligibilityAssessment(
        anHdcPrisonerSearchResult.copy(
          paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
          actualParoleDate = LocalDate.now(clock).plusDays(1),
        ),
        HdcStatuses(rejectedHdcStatuses),
      )

      assertThat(result.isEligible).isFalse()
      assertThat(result.genericIneligibilityReasons).isEmpty()
      assertThat(result.crdIneligibilityReasons).containsExactly("is on non-eligible EDS")
      assertThat(result.prrdIneligibilityReasons).containsExactly("has no post recall release date")
      assertThat(result.hdcIneligibilityReasons).containsExactly("is not expected to be released on HDC")
      assertThat(result.eligibleKind).isNull()
    }
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-11-03T00:00:00Z"), ZoneId.systemDefault())

    val aPrisonerSearchResult = prisonerSearchResult(
      conditionalReleaseDate = LocalDate.now(clock).plusDays(1),
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = LocalDate.now(clock).plusDays(1),
    ).copy(
      topupSupervisionStartDate = LocalDate.now(clock).plusYears(1),
      topupSupervisionExpiryDate = LocalDate.now(clock).plusYears(1),
      releaseDate = LocalDate.now(clock).plusDays(1),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    )

    val aRecallPrisonerSearchResult = prisonerSearchResult(
      conditionalReleaseDate = null,
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = null,
      postRecallReleaseDate = LocalDate.now(clock).plusDays(1),
    ).copy(
      topupSupervisionStartDate = null,
      releaseDate = LocalDate.now(clock).plusDays(1),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
    )

    val anHdcPrisonerSearchResult = prisonerSearchResult(
      conditionalReleaseDate = LocalDate.now(clock).plusMonths(1),
      sentenceStartDate = LocalDate.parse("2023-09-14"),
      confirmedReleaseDate = LocalDate.now(clock).plusMonths(1),
    ).copy(
      topupSupervisionStartDate = LocalDate.now(clock).plusYears(1),
      topupSupervisionExpiryDate = LocalDate.now(clock).plusYears(1),
      releaseDate = LocalDate.now(clock).plusDays(1),
      sentenceExpiryDate = LocalDate.parse("2024-09-14"),
      homeDetentionCurfewEligibilityDate = LocalDate.now(clock).minusYears(1),
      homeDetentionCurfewActualDate = LocalDate.now(clock).plusDays(1),
    )
  }
}
