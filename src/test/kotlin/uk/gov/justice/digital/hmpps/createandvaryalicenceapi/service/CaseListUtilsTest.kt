package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ManagedCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isBreachOfTopUpSupervision
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isEligibleEDS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isParoleEligible
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isRecall
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class CaseListUtilsTest {

  @Nested
  inner class IsParoleEligible {
    @Test
    fun `should return false if ped is null`() {
      val ped = null
      assertThat(isParoleEligible(ped, clock)).isFalse()
    }

    @Test
    fun `should return false if ped is less than today`() {
      val ped = yesterday
      assertThat(isParoleEligible(ped, clock)).isFalse()
    }

    @Test
    fun `should return false if ped is equal to today`() {
      val ped = LocalDate.now(clock)
      assertThat(isParoleEligible(ped, clock)).isFalse()
    }

    @Test
    fun `should return true if ped is greater than today`() {
      val ped = oneDayFromNow
      assertThat(isParoleEligible(ped, clock)).isTrue()
    }
  }

  @Nested
  inner class IsEligibleEDS {
    @Test
    fun `returns true when PED is not set`() {
      assertThat(isEligibleEDS(null, null, null, null, clock)).isTrue()
    }

    @Test
    fun `returns false when PED is set and CRD is not`() {
      assertThat(isEligibleEDS(oneDayFromNow, null, null, null, clock)).isFalse()
    }

    @Test
    fun `returns false when PED is in the future`() {
      assertThat(isEligibleEDS(oneDayFromNow, twoDayFromNow, null, null, clock)).isFalse()
    }

    @Test
    fun `returns true if past PED and ARD is within 4 days of CRD`() {
      assertThat(isEligibleEDS(yesterday, tenDaysFromNow, LocalDate.now(clock).plusDays(6), null, clock)).isTrue()
    }

    @Test
    fun `returns true if past PED and ARD is equal to CRD`() {
      assertThat(isEligibleEDS(yesterday, tenDaysFromNow, tenDaysFromNow, null, clock)).isTrue()
    }

    @Test
    fun `returns false if past PED and ARD is more than 4 days before CRD`() {
      assertThat(isEligibleEDS(yesterday, tenDaysFromNow, LocalDate.now(clock).plusDays(5), null, clock)).isFalse()
    }

    @Test
    fun `returns true if past PED and ARD not set`() {
      assertThat(isEligibleEDS(yesterday, tenDaysFromNow, null, null, clock)).isTrue()
    }

    @Test
    fun `returns false if APD is set`() {
      assertThat(isEligibleEDS(yesterday, tenDaysFromNow, tenDaysFromNow, nineDaysFromNow, clock)).isFalse()
    }
  }

  @Nested
  inner class IsRecall {
    @Test
    fun `returns false if CRD is set and not PRRD`() {
      assertThat(isRecall(managedCase.copy(nomisRecord = Prisoner(postRecallReleaseDate = null)))).isFalse()
    }

    @Test
    fun `returns false if PRRD is before CRD`() {
      assertThat(
        isRecall(
          managedCase.copy(
            nomisRecord = Prisoner(
              postRecallReleaseDate = yesterday,
              conditionalReleaseDate = oneDayFromNow,
            ),
          ),
        ),
      ).isFalse()
    }

    @Test
    fun `returns false if PRRD is equal to CRD`() {
      assertThat(
        isRecall(
          managedCase.copy(
            nomisRecord = Prisoner(
              postRecallReleaseDate = oneDayFromNow,
              conditionalReleaseDate = oneDayFromNow,
            ),
          ),
        ),
      ).isFalse()
    }

    @Test
    fun `returns true if PRRD is after CRD`() {
      assertThat(
        isRecall(
          managedCase.copy(
            nomisRecord = Prisoner(
              postRecallReleaseDate = oneDayFromNow,
              conditionalReleaseDate = yesterday,
            ),
          ),
        ),
      ).isTrue()
    }
  }

  @Nested
  inner class IsBreachOfTopUpSupervision {
    @Test
    fun `returns false if imprisonmentStatus is not set`() {
      assertThat(isBreachOfTopUpSupervision(managedCase)).isFalse()
    }

    @Test
    fun `returns false if imprisonmentStatus is not equal to BOTUS`() {
      assertThat(
        isBreachOfTopUpSupervision(
          managedCase.copy(
            nomisRecord = Prisoner(
              imprisonmentStatus = "NOT",
            ),
          ),
        ),
      ).isFalse()
    }

    @Test
    fun `returns true if imprisonmentStatus is equal to BOTUS`() {
      assertThat(
        isBreachOfTopUpSupervision(
          managedCase.copy(
            nomisRecord = Prisoner(
              imprisonmentStatus = "BOTUS",
            ),
          ),
        ),
      ).isTrue()
    }
  }

  private companion object {
    private fun createClock(timestamp: String) = Clock.fixed(Instant.parse(timestamp), ZoneId.systemDefault())

    val dateTime = LocalDateTime.of(LocalDate.now(), LocalTime.of(15, 13, 39))
    val instant = dateTime.atZone(ZoneId.systemDefault()).toInstant()
    val clock: Clock = createClock(instant.toString())
    val yesterday = LocalDate.now(clock).minusDays(1)
    val oneDayFromNow = LocalDate.now(clock).plusDays(1)
    val twoDayFromNow = LocalDate.now(clock).plusDays(2)
    val nineDaysFromNow = LocalDate.now(clock).plusDays(9)
    val tenDaysFromNow = LocalDate.now(clock).plusDays(10)

    val managedCase = ManagedCase(
      nomisRecord = Prisoner(
        recall = true,
        conditionalReleaseDate = oneDayFromNow,
        postRecallReleaseDate = twoDayFromNow,
      ),
      cvlFields = CvlFields(
        licenceType = LicenceType.AP,
      ),
    )
  }
}
