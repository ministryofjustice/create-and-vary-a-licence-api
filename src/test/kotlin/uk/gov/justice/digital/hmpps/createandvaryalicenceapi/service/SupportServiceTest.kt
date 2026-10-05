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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Is91SupportInfo.Is91CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.RemandSupportInfo.RemandCourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.IS91DeterminationService.Is91CourtEvents
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.hdcPrisonerStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.hdc.HdcStatuses
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.BookingSentenceAndRecallTypes
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.RecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceAndRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceRecallType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.support.SupportService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.RemandCourtEvents
import java.time.LocalDate

class SupportServiceTest {
  private val prisonerSearchApiClient = mock<PrisonerSearchApiClient>()
  private val prisonService = mock<PrisonService>()
  private val eligibilityService = mock<EligibilityService>()
  private val iS91DeterminationService = mock<IS91DeterminationService>()
  private val hdcService = mock<HdcService>()

  private val service = SupportService(
    prisonerSearchApiClient,
    prisonService,
    eligibilityService,
    iS91DeterminationService,
    hdcService,
  )

  @BeforeEach
  fun reset() {
    reset(
      prisonerSearchApiClient,
      prisonService,
      eligibilityService,
      iS91DeterminationService,
      hdcService,
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
    val hdcStatuses = HdcStatuses(
      listOf(
        hdcPrisonerStatus().copy(
          bookingId = hdcPrisoner.bookingId?.toLong(),
          approvalStatus = "APPROVED",
        ),
      ),
    )

    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA"))).thenReturn(listOf(hdcPrisoner))
    whenever(hdcService.getHdcStatus(listOf(hdcPrisoner))).thenReturn(hdcStatuses)
    whenever(eligibilityService.getEligibilityAssessment(eq(hdcPrisoner), eq(hdcStatuses))).thenReturn(
      EligibilityAssessment(
        genericIneligibilityReasons = listOf("A reason", "Approved for HDC"),
        crdIneligibilityReasons = emptyList(),
        prrdIneligibilityReasons = emptyList(),
        isEligible = false,
        eligibleKind = null,
        ineligibilityReasons = listOf("A reason", "Approved for HDC"),
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
  fun `get recall info returns standard recall`() {
    val bookingSentenceAndRecallTypes = BookingSentenceAndRecallTypes(
      bookingId = 123456L,
      sentenceTypeRecallTypes = listOf(
        SentenceAndRecallType(
          "LR",
          SentenceRecallType("Standard Recall", isStandardRecall = true, isFixedTermRecall = false),
        ),
        SentenceAndRecallType(
          "ADIMP_ORA",
          SentenceRecallType("Other", isStandardRecall = false, isFixedTermRecall = false),
        ),
        SentenceAndRecallType(
          "ADIMP_ORA",
          SentenceRecallType("None", isStandardRecall = false, isFixedTermRecall = false),
        ),
      ),
    )

    whenever(prisonService.getSentenceAndRecallTypes(123456L)).thenReturn(bookingSentenceAndRecallTypes)
    whenever(prisonService.getRecallType(bookingSentenceAndRecallTypes)).thenReturn(RecallType.STANDARD)

    val result = service.getRecallInfo(123456L)

    assertThat(result.recallType).isEqualTo(RecallType.STANDARD)
    assertThat(result.recallName).isEqualTo("Standard Recall")
    assertThat(result.standardRecallSentenceTypes).containsExactly("LR")
    assertThat(result.fixTermSentenceTypes).isEmpty()
    assertThat(result.otherSentenceTypes).containsExactly("ADIMP_ORA")
  }

  @Test
  fun `get recall info returns fixed term recall`() {
    val bookingSentenceAndRecallTypes = BookingSentenceAndRecallTypes(
      bookingId = 123456L,
      sentenceTypeRecallTypes = listOf(
        SentenceAndRecallType(
          "FTR_ORA",
          SentenceRecallType("14 Day Fixed Term Recall", isStandardRecall = false, isFixedTermRecall = true),
        ),
      ),
    )

    whenever(prisonService.getSentenceAndRecallTypes(123456L)).thenReturn(bookingSentenceAndRecallTypes)
    whenever(prisonService.getRecallType(bookingSentenceAndRecallTypes)).thenReturn(RecallType.FIXED_TERM)

    val result = service.getRecallInfo(123456L)

    assertThat(result.recallType).isEqualTo(RecallType.FIXED_TERM)
    assertThat(result.recallName).isEqualTo("14 Day Fixed Term Recall")
    assertThat(result.fixTermSentenceTypes).containsExactly("FTR_ORA")
    assertThat(result.standardRecallSentenceTypes).isEmpty()
    assertThat(result.otherSentenceTypes).isEmpty()
  }

  @Test
  fun `get remand info returns expected false if not on remand`() {
    whenever(
      prisonService.getCourtOutcomeEvents(
        listOf(123456L),
        RemandCourtEvents.getCodes(),
      ),
    ).thenReturn(emptyList())

    val result = service.getRemandInfo(123456L)

    assertThat(result.isRemand).isFalse()
    assertThat(result.courtEventOutcomeCode).isNull()
    assertThat(result.courtEventOutcomeDescription).isNull()
  }

  @Test
  fun `get remand info returns a correctly populated object`() {
    whenever(
      prisonService.getCourtOutcomeEvents(
        listOf(123456L),
        RemandCourtEvents.getCodes(),
      ),
    ).thenReturn(
      listOf(
        CourtEventOutcome(
          bookingId = 123456L,
          eventId = 1L,
          outcomeReasonCode = "4531",
        ),
      ),
    )

    val result = service.getRemandInfo(123456L)

    assertThat(result.isRemand).isTrue()
    assertThat(result.courtEventOutcomeCode).isEqualTo("4531")
    assertThat(result.courtEventOutcomeDescription).isEqualTo("Remand in Custody (Bail Refused)")
    assertThat(result.remandCourtEventOutcomes).containsExactly(
      RemandCourtEventOutcome(
        "4531",
        "Remand in Custody (Bail Refused)",
      ),
    )
  }

  @Test
  fun `get IS91 info returns a correctly populated object`() {
    whenever(
      prisonService.getCourtOutcomeEvents(
        listOf(123456L),
        Is91CourtEvents.getCodes(),
      ),
    ).thenReturn(
      listOf(
        CourtEventOutcome(
          bookingId = 123456L,
          eventId = 1L,
          outcomeReasonCode = "3006",
        ),
      ),
    )

    val result = service.getIs91Info(123456L)

    assertThat(result.isIs91Case).isTrue()
    assertThat(result.is91CourtEventOutcomes).containsExactly(
      Is91CourtEventOutcome(
        "3006",
        "Deportation recommended",
      ),
    )
  }

  @Test
  fun `get support info returns a correctly populated object`() {
    whenever(prisonerSearchApiClient.searchPrisonersByNomisIds(listOf("A1234AA")))
      .thenReturn(listOf(aPrisonerSearchResult))

    val bookingSentenceAndRecallTypes = BookingSentenceAndRecallTypes(
      bookingId = 123456L,
      sentenceTypeRecallTypes = emptyList(),
    )

    whenever(prisonService.getSentenceAndRecallTypes(123456L))
      .thenReturn(bookingSentenceAndRecallTypes)
    whenever(prisonService.getRecallType(bookingSentenceAndRecallTypes))
      .thenReturn(RecallType.NONE)

    whenever(prisonService.getCourtOutcomeEvents(listOf(123456L), RemandCourtEvents.getCodes()))
      .thenReturn(
        listOf(
          CourtEventOutcome(
            bookingId = 123456L,
            eventId = 1L,
            outcomeReasonCode = "4531",
          ),
        ),
      )

    whenever(
      prisonService.getCourtOutcomeEvents(
        listOf(123456L),
        Is91CourtEvents.getCodes(),
      ),
    ).thenReturn(
      listOf(
        CourtEventOutcome(
          bookingId = 123456L,
          eventId = 1L,
          outcomeReasonCode = "3006",
        ),
      ),
    )

    val result = service.getSupportInfo("A1234AA")

    assertThat(result.isIS91Case).isTrue()
    assertThat(result.recallSupportInfo?.recallType).isEqualTo(RecallType.NONE)
    assertThat(result.remandSupportInfo?.isRemand).isTrue()
    assertThat(result.is91SupportInfo?.isIs91Case).isTrue()
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
