package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.ProbationPractitioner.Companion.restrictedView
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.response.FoundComCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.OffenceHistory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.PrisonApiPrisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.prison.SentenceDetail
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.ElectronicMonitoringProviderStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ElectronicMonitoringProvider as EntityElectronicMonitoringProvider

class ToModelTransformersTest {

  private lateinit var electronicMonitoringProvider: EntityElectronicMonitoringProvider

  @BeforeEach
  fun setup() {
    electronicMonitoringProvider = EntityElectronicMonitoringProvider(
      licence = TestData.createCrdLicence(),
      isToBeTaggedForProgramme = true,
      programmeName = "some programme",
    )
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns NOT_NEEDED when provider is null`() {
    val status = determineElectronicMonitoringProviderStatus(null)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.NOT_NEEDED)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns NOT_STARTED when isToBeTaggedForProgramme is null`() {
    electronicMonitoringProvider.isToBeTaggedForProgramme = null

    val status = determineElectronicMonitoringProviderStatus(electronicMonitoringProvider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.NOT_STARTED)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns COMPLETE when isToBeTaggedForProgramme is true`() {
    val status = determineElectronicMonitoringProviderStatus(electronicMonitoringProvider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
  }

  @Test
  fun `determineElectronicMonitoringProviderStatus returns COMPLETE when isToBeTaggedForProgramme is false`() {
    with(electronicMonitoringProvider) {
      isToBeTaggedForProgramme = false
      programmeName = null
    }
    val status = determineElectronicMonitoringProviderStatus(electronicMonitoringProvider)
    assertThat(status).isEqualTo(ElectronicMonitoringProviderStatus.COMPLETE)
  }

  @Test
  fun `restrictedCase creates a restricted case with correct fields`() {
    val restrictedCase = FoundComCase.restrictedCase(
      kind = LicenceKind.CRD,
      crn = "X12345",
      isOnProbation = true,
      releaseDate = LocalDate.now().plusDays(1),
    )

    assertThat(restrictedCase.name).isEqualTo("Access restricted on NDelius")
    assertThat(restrictedCase.comName).isEqualTo("Restricted")
    assertThat(restrictedCase.probationPractitioner).isEqualTo(restrictedView())
    assertThat(restrictedCase.teamName).isEqualTo("Restricted")
    assertThat(restrictedCase.isRestricted).isTrue()
  }

  @Test
  fun `restrictedCase handles null crn`() {
    val restrictedCase = FoundComCase.restrictedCase(
      kind = LicenceKind.VARIATION,
      crn = null,
      isOnProbation = false,
      releaseDate = LocalDate.now().plusDays(1),
    )

    assertThat(restrictedCase.name).isEqualTo("Access restricted on NDelius")
    assertThat(restrictedCase.comName).isEqualTo("Restricted")
    assertThat(restrictedCase.probationPractitioner).isEqualTo(restrictedView())
    assertThat(restrictedCase.isRestricted).isTrue()
  }

  @Test
  fun `toPrisonerSearchPrisoner maps all fields correctly`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "A1234BC",
      firstName = "John",
      middleName = "Michael",
      lastName = "Smith",
      bookingId = 12345L,
      legalStatus = "SENTENCED",
      offenceHistory = listOf(
        OffenceHistory(
          offenceDescription = "Theft",
          offenceCode = "TH001",
          mostSerious = false,
        ),
        OffenceHistory(
          offenceDescription = "Burglary",
          offenceCode = "BU001",
          mostSerious = true,
        ),
      ),
      agencyId = "MDI",
      status = "ACTIVE IN",
      dateOfBirth = LocalDate.of(1990, 5, 15),
      sentenceDetail = SentenceDetail(
        sentenceStartDate = LocalDate.of(2023, 1, 1),
        sentenceExpiryDate = LocalDate.of(2025, 12, 31),
        conditionalReleaseDate = LocalDate.of(2024, 6, 15),
        conditionalReleaseOverrideDate = LocalDate.of(2024, 7, 1),
        confirmedReleaseDate = LocalDate.of(2024, 7, 1),
        homeDetentionCurfewEligibilityDate = LocalDate.of(2024, 3, 1),
        homeDetentionCurfewActualDate = LocalDate.of(2024, 3, 15),
        licenceExpiryDate = LocalDate.of(2025, 6, 30),
        topupSupervisionStartDate = LocalDate.of(2025, 1, 1),
        topupSupervisionExpiryDate = LocalDate.of(2025, 12, 31),
        topupSupervisionExpiryOverrideDate = LocalDate.of(2026, 1, 15),
        paroleEligibilityDate = LocalDate.of(2024, 4, 1),
        paroleEligibilityOverrideDate = LocalDate.of(2024, 4, 15),
        postRecallReleaseDate = LocalDate.of(2024, 8, 1),
        postRecallReleaseOverrideDate = LocalDate.of(2024, 8, 15),
      ),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    assertThat(result.prisonerNumber).isEqualTo("A1234BC")
    assertThat(result.bookingId).isEqualTo("12345")
    assertThat(result.firstName).isEqualTo("John")
    assertThat(result.middleNames).isEqualTo("Michael")
    assertThat(result.lastName).isEqualTo("Smith")
    assertThat(result.dateOfBirth).isEqualTo(LocalDate.of(1990, 5, 15))
    assertThat(result.legalStatus).isEqualTo("SENTENCED")
    assertThat(result.mostSeriousOffence).isEqualTo("Burglary")
    assertThat(result.conditionalReleaseDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(result.confirmedReleaseDate).isEqualTo(LocalDate.of(2024, 7, 1))
    assertThat(result.homeDetentionCurfewEligibilityDate).isEqualTo(LocalDate.of(2024, 3, 1))
    assertThat(result.homeDetentionCurfewActualDate).isEqualTo(LocalDate.of(2024, 3, 15))
    assertThat(result.topupSupervisionStartDate).isEqualTo(LocalDate.of(2025, 1, 1))
    assertThat(result.topupSupervisionExpiryDate).isEqualTo(LocalDate.of(2026, 1, 15))
    assertThat(result.paroleEligibilityDate).isEqualTo(LocalDate.of(2024, 4, 15))
    assertThat(result.postRecallReleaseDate).isEqualTo(LocalDate.of(2024, 8, 15))
    assertThat(result.sentenceExpiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
    assertThat(result.licenceExpiryDate).isEqualTo(LocalDate.of(2025, 6, 30))
    assertThat(result.sentenceStartDate).isEqualTo(LocalDate.of(2023, 1, 1))
    assertThat(result.prisonId).isEqualTo("MDI")
    assertThat(result.status).isEqualTo("ACTIVE IN")
  }

  @Test
  fun `toPrisonerSearchPrisoner uses override dates when present`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "B5678CD",
      firstName = "Jane",
      lastName = "Doe",
      bookingId = 67890L,
      offenceHistory = emptyList(),
      agencyId = "LEI",
      status = "ACTIVE OUT",
      dateOfBirth = LocalDate.of(1985, 8, 20),
      sentenceDetail = SentenceDetail(
        conditionalReleaseDate = LocalDate.of(2024, 5, 1),
        conditionalReleaseOverrideDate = LocalDate.of(2024, 6, 1),
        topupSupervisionExpiryDate = LocalDate.of(2025, 12, 31),
        topupSupervisionExpiryOverrideDate = LocalDate.of(2026, 1, 1),
        paroleEligibilityDate = LocalDate.of(2024, 3, 1),
        paroleEligibilityOverrideDate = LocalDate.of(2024, 4, 1),
        postRecallReleaseDate = LocalDate.of(2024, 7, 1),
        postRecallReleaseOverrideDate = LocalDate.of(2024, 8, 1),
      ),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    // Override dates should be used when present
    assertThat(result.conditionalReleaseDate).isEqualTo(LocalDate.of(2024, 6, 1))
    assertThat(result.topupSupervisionExpiryDate).isEqualTo(LocalDate.of(2026, 1, 1))
    assertThat(result.paroleEligibilityDate).isEqualTo(LocalDate.of(2024, 4, 1))
    assertThat(result.postRecallReleaseDate).isEqualTo(LocalDate.of(2024, 8, 1))
  }

  @Test
  fun `toPrisonerSearchPrisoner uses standard dates when override dates are null`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "C9012EF",
      firstName = "Bob",
      lastName = "Jones",
      bookingId = 11111L,
      offenceHistory = emptyList(),
      agencyId = "BMI",
      status = "INACTIVE OUT",
      dateOfBirth = LocalDate.of(1992, 11, 5),
      sentenceDetail = SentenceDetail(
        conditionalReleaseDate = LocalDate.of(2024, 5, 1),
        conditionalReleaseOverrideDate = null,
        topupSupervisionExpiryDate = LocalDate.of(2025, 12, 31),
        topupSupervisionExpiryOverrideDate = null,
        paroleEligibilityDate = LocalDate.of(2024, 3, 1),
        paroleEligibilityOverrideDate = null,
        postRecallReleaseDate = LocalDate.of(2024, 7, 1),
        postRecallReleaseOverrideDate = null,
      ),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    // Standard dates should be used when override dates are null
    assertThat(result.conditionalReleaseDate).isEqualTo(LocalDate.of(2024, 5, 1))
    assertThat(result.topupSupervisionExpiryDate).isEqualTo(LocalDate.of(2025, 12, 31))
    assertThat(result.paroleEligibilityDate).isEqualTo(LocalDate.of(2024, 3, 1))
    assertThat(result.postRecallReleaseDate).isEqualTo(LocalDate.of(2024, 7, 1))
  }

  @Test
  fun `toPrisonerSearchPrisoner handles null middle name`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "D3456GH",
      firstName = "Alice",
      middleName = null,
      lastName = "Brown",
      bookingId = 22222L,
      offenceHistory = emptyList(),
      agencyId = "BXI",
      status = "ACTIVE IN",
      dateOfBirth = LocalDate.of(1988, 2, 14),
      sentenceDetail = SentenceDetail(),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    assertThat(result.middleNames).isNull()
  }

  @Test
  fun `toPrisonerSearchPrisoner finds most serious offence`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "E7890IJ",
      firstName = "Charlie",
      lastName = "Wilson",
      bookingId = 33333L,
      offenceHistory = listOf(
        OffenceHistory(offenceDescription = "Minor theft", offenceCode = "TH001", mostSerious = false),
        OffenceHistory(offenceDescription = "Robbery", offenceCode = "RB001", mostSerious = true),
        OffenceHistory(offenceDescription = "Assault", offenceCode = "AS001", mostSerious = false),
      ),
      agencyId = "WDI",
      status = "ACTIVE IN",
      dateOfBirth = LocalDate.of(1995, 7, 22),
      sentenceDetail = SentenceDetail(),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    assertThat(result.mostSeriousOffence).isEqualTo("Robbery")
  }

  @Test
  fun `toPrisonerSearchPrisoner handles no most serious offence`() {
    val prisonApiPrisoner = PrisonApiPrisoner(
      offenderNo = "F1234KL",
      firstName = "David",
      lastName = "Taylor",
      bookingId = 44444L,
      offenceHistory = listOf(
        OffenceHistory(offenceDescription = "Theft", offenceCode = "TH001", mostSerious = false),
        OffenceHistory(offenceDescription = "Fraud", offenceCode = "FR001", mostSerious = false),
      ),
      agencyId = "HMP",
      status = "ACTIVE IN",
      dateOfBirth = LocalDate.of(1980, 12, 1),
      sentenceDetail = SentenceDetail(),
    )

    val result = prisonApiPrisoner.toPrisonerSearchPrisoner()

    assertThat(result.mostSeriousOffence).isNull()
  }
}
