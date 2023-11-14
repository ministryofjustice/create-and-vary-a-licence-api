package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EligibilityServiceTest {

  private val service =
    EligibilityService(
      clock,
    )

  @Test
  fun `Person is eligible for CVL`() {
    val result = service.isEligibleForCvl(aPrisonerSearchResult)
    assertThat(result).isTrue()
  }

  @Test
  fun `Person is parole eligible but parole eligibility date is in the past - eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).minusDays(1),
      ),
    )
    assertThat(result).isTrue()
  }

  @Test
  fun `Person is parole eligible - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).plusYears(1),
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person has an incorrect legal status - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        legalStatus = "DEAD",
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on an indeterminate sentence - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        indeterminateSentence = true,
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person does not have a conditional release date - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        conditionalReleaseDate = null,
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - ARD is outside threshold in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = LocalDate.now(clock).minusDays(5),
      ),
    )
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
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - has a APD with a PED today - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock),
        actualParoleDate = LocalDate.now(clock),
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on ineligible EDS - has a APD with a PED in the future - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        paroleEligibilityDate = LocalDate.now(clock).plusDays(1),
        actualParoleDate = LocalDate.now(clock).plusDays(1),
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is an inactive transfer - eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        status = "INACTIVE TRN",
      ),
    )
    assertThat(result).isTrue()
  }

  @Test
  fun `Person does not have an active prison status - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        status = "INACTIVE OUT",
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person has a confirmed release date (ARD) in the past - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        confirmedReleaseDate = LocalDate.now(clock).minusDays(1),
      ),
    )
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
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on recall with a post recall release date (PRRD) before CRD - eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        postRecallReleaseDate = LocalDate.now(clock).minusDays(1),
      ),
    )
    assertThat(result).isTrue()
  }

  @Test
  fun `Person is on recall with a post recall release date (PRRD) after CRD - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        postRecallReleaseDate = LocalDate.now(clock).plusDays(2),
      ),
    )
    assertThat(result).isFalse()
  }

  @Test
  fun `Person is on recall with a recall flag - not eligible for CVL `() {
    val result = service.isEligibleForCvl(
      aPrisonerSearchResult.copy(
        conditionalReleaseDate = null,
        recall = true,
      ),
    )
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
      recall = false,
    )
  }
}
