package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remand

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.CourtEventOutcome
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonerSearchPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.remand.RemandCourtEvents.Companion.REMAND_CODES
import java.time.LocalDate

class RemandIdentificationServiceTest {

  private val prisonApiClient = mock<PrisonApiClient>()
  private val service = RemandIdentificationService(prisonApiClient)

  @BeforeEach
  fun reset() {
    reset(prisonApiClient)
  }

  @ParameterizedTest(name = "Returns true for outcome reason code {0}")
  @ValueSource(
    strings = [
      "2507", "4001", "4004", "4012", "4016", "4505", "4506", "4531", "4532",
      "4534", "4535", "4536", "4537", "4539", "4549", "4553", "4554", "4560",
      "4561", "4563", "4564", "4565", "4570", "4571", "4588", "5601",
    ],
  )
  fun `returns true when prisoner has a remand court event outcome`(outcomeReasonCode: String) {
    whenever(
      prisonApiClient.getCourtEventOutcomes(listOf(54321L), REMAND_CODES),
    ).thenReturn(listOf(CourtEventOutcome(bookingId = 54321L, eventId = 1L, outcomeReasonCode = outcomeReasonCode)))
    assertThat(service.isRemandBooking(aPrisoner.copy(bookingId = "54321"))).isTrue()
  }

  @Test
  fun `returns false when prisoner has no remand court event outcomes`() {
    whenever(
      prisonApiClient.getCourtEventOutcomes(listOf(54321L), REMAND_CODES),
    ).thenReturn(emptyList())
    assertThat(service.isRemandBooking(aPrisoner.copy(bookingId = "54321"))).isFalse()
  }

  @Test
  fun `returns false when prisoner has no booking Id`() {
    assertThat(service.isRemandBooking(aPrisoner.copy(bookingId = null))).isFalse()
  }

  private val aPrisoner = PrisonerSearchPrisoner(
    prisonerNumber = "A1234AA",
    bookingId = "54321",
    mostSeriousOffence = "Robbery",
    firstName = "Jane",
    lastName = "Doe",
    dateOfBirth = LocalDate.parse("1985-01-01"),
  )
}
