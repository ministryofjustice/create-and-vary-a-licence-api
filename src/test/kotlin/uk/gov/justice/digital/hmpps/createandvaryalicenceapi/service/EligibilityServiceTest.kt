package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerHdcStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EligibilityServiceTest {
  private val prisonApiClient = mock<PrisonApiClient>()

  private val service =
    EligibilityService(
      prisonApiClient,
      clock,
    )

  @BeforeEach
  fun reset() {
    reset(
      prisonApiClient,
    )
  }

  @Test
  fun `Person is eligible for CVL`() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus,
      ),
    )

    val result = service.isEligibleForCvl(aPrisonerSearchResult)
    verify(prisonApiClient).getHdcStatus(54321)

    assertThat(result).isTrue()
  }

  @Test
  fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus,
      ),
    )

    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
      ),
    )
    verify(prisonApiClient).getHdcStatus(54321L)

    assertThat(result).isTrue()
  }

  @Test
  fun `Person is parole eligible - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person has an incorrect legal status - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        legalStatus = "DEAD",
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on an indeterminate sentence - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        indeterminateSentence = true,
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person does not have a conditional release date - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        conditionalReleaseDate = null,
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - ARD is outside threshold in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = LocalDate.now(clock).minusDays(5),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - ARD is outside threshold in the future - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        confirmedReleaseDate = LocalDate.now(clock).plusDays(2),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - has a APD and a PED in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        actualParoleDate = LocalDate.now(clock).plusDays(1),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - has a APD with a PED in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
        actualParoleDate = LocalDate.now(clock).plusDays(1),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is an inactive transfer - eligible for CVL `() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus,
      ),
    )

    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        status = "INACTIVE TRN",
      ),
    )
    verify(prisonApiClient).getHdcStatus(54321)

    assertThat(result).isTrue()
  }

  @Test
  fun `Person does not have an active prison status - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person has a confirmed release date (ARD) in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = LocalDate.now(clock).minusDays(1),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person has a conditional release date (CRD) in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        conditionalReleaseDate = LocalDate.now(clock).minusDays(1),
        confirmedReleaseDate = null,
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on recall with a post recall release date (PRRD) before CRD - eligible for CVL `() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus,
      ),
    )

    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
      ),
    )
    verify(prisonApiClient).getHdcStatus(54321)

    assertThat(result).isTrue()
  }

  @Test
  fun `Person is on recall with a post recall release date (PRRD) after CRD - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        postRecallReleaseDate = LocalDate.now(clock).plusDays(2),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on HDC - approved HDC status - not eligible for CVL `() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus.copy(
          approvalStatus = "APPROVED",
        ),
      ),
    )

    val result = service.isEligibleForCvl(
      aPrisonerSearchResult,
    )
    verify(prisonApiClient).getHdcStatus(54321)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on HDC - has a HDCED - not eligible for CVL `() {
    whenever(prisonApiClient.getHdcStatus(54321)).thenReturn(
      Mono.just(
        aPrisonerHdcStatus,
      ),
    )

    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        homeDetentionCurfewEligibilityDate = LocalDate.now(clock).plusYears(1),
      ),
    )
    verify(prisonApiClient).getHdcStatus(54321)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person has no ARD and a CRD in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = null,
        conditionalReleaseDate = LocalDate.now(clock).minusDays(10),
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  @Test
  fun `Person has no ARD and no CRD - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = null,
        conditionalReleaseDate = null,
      ),
    )
    verifyNoInteractions(prisonApiClient)

    assertThat(result).isFalse()
  }

  private companion object {
    val clock: Clock = Clock.fixed(Instant.parse("2023-11-03T00:00:00Z"), ZoneId.systemDefault())

    val aPrisonerSearchResult = PrisonerSearchPrisoner(
      prisonerNumber = "A1234AA",
      bookingId = "54321",
      status = "ACTIVE IN",
      mostSeriousOffence = "Robbery",
      licenceExpiryDate = LocalDate.now(clock).plusYears(1),
      topUpSupervisionExpiryDate = LocalDate.now(clock).plusYears(1),
      homeDetentionCurfewEligibilityDate = null,
      releaseDate = LocalDate.now(clock).plusDays(1),
      confirmedReleaseDate = LocalDate.now(clock).plusDays(1),
      conditionalReleaseDate = LocalDate.now(clock).plusDays(1),
      paroleEligibilityDate = null,
      actualParoleDate = null,
      postRecallReleaseDate = null,
      legalStatus = "SENTENCED",
      indeterminateSentence = false,
    )

    val aPrisonerHdcStatus = PrisonerHdcStatus(
      approvalStatusDate = null,
      approvalStatus = "REJECTED",
      refusedReason = null,
      checksPassedDate = null,
      bookingId = 54321,
      passed = true,
    )
  }
}
