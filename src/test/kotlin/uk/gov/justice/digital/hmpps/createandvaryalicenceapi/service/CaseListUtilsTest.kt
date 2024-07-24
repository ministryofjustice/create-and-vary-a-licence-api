package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.CvlFields
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.isBreachOfTopUpSupervision
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
  inner class IsRecall {
    @Test
    fun `returns false if CRD is set and not PRRD`() {
      assertThat(
        isRecall(
          managedCase.copy(
            nomisRecord = managedCase.nomisRecord?.copy(
              postRecallReleaseDate = null,
            ),
          ),
        ),
      ).isFalse()
    }

    @Test
    fun `returns false if PRRD is before CRD`() {
      assertThat(
        isRecall(
          managedCase.copy(
            nomisRecord = managedCase.nomisRecord?.copy(
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
            nomisRecord = managedCase.nomisRecord?.copy(
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
            nomisRecord = managedCase.nomisRecord?.copy(
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
            nomisRecord = managedCase.nomisRecord?.copy(
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
            nomisRecord = managedCase.nomisRecord?.copy(
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

    val managedCase = ManagedCase(
      nomisRecord = Prisoner(
        recall = true,
        conditionalReleaseDate = oneDayFromNow,
        postRecallReleaseDate = twoDayFromNow,
        firstName = "Steve",
        lastName = "Cena",
        prisonerNumber = "AB1234E",
        status = "ACTIVE IN",
        legalStatus = "IMMIGRATION_DETAINEE",
        dateOfBirth = LocalDate.of(1985, 12, 28),
        mostSeriousOffence = "Robbery",
      ),
      cvlFields = CvlFields(
        licenceType = LicenceType.AP,
      ),
    )
  }
}
