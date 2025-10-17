package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs.determineCaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.ATTENTION_NEEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.FUTURE_RELEASES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

class TabsTest {

  private val fixedNow = LocalDate.of(2025, 7, 8)
  private val clock: Clock = Clock.fixed(fixedNow.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC)

  @ParameterizedTest
  @CsvSource("IN_PROGRESS", "SUBMITTED", "APPROVED", "NOT_STARTED")
  fun `returns ATTENTION_NEEDED when licence is inflight and start date is null`(status: LicenceStatus) {
    // Given
    val licence = aLicenceCaseInformation(licenceStatus = status)

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = null,
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns ATTENTION_NEEDED when licence is approved and start date is in the past`() {
    // Given
    val licence = aLicenceCaseInformation(licenceStatus = APPROVED)

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.minusDays(1),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `does not return ATTENTION_NEEDED when licence is approved and start date is today`() {
    // Given
    val licence = aLicenceCaseInformation(licenceStatus = APPROVED)

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow,
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isNotEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `does not return ATTENTION_NEEDED when licence is approved and start date is tomorrow`() {
    // Given
    val licence = aLicenceCaseInformation(licenceStatus = APPROVED)

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(1),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isNotEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns ATTENTION_NEEDED when no licence and no LSD`() {
    // Given
    val licence: LicenceCase? = null

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = null,
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns RELEASES_IN_NEXT_TWO_WORKING_DAYS when licence LSD is not in past and due to be released soon`() {
    // Given
    val licence = aLicenceCaseInformation(
      licenceStatus = APPROVED,
    )

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(1),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(RELEASES_IN_NEXT_TWO_WORKING_DAYS)
  }

  @Test
  fun `returns RELEASES_IN_NEXT_TWO_WORKING_DAYS when no licence and falls back to CVL info`() {
    // Given
    val licence: LicenceCase? = null

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(2),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(RELEASES_IN_NEXT_TWO_WORKING_DAYS)
  }

  @Test
  fun `returns FUTURE_RELEASES when licence present`() {
    // Given
    val licence = aLicenceCaseInformation(
      licenceStatus = APPROVED,
    )

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.plusDays(3),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(FUTURE_RELEASES)
  }

  @Test
  fun `returns FUTURE_RELEASES when no licence and falls back to CVL info`() {
    // Given
    val licence: LicenceCase? = null

    // When
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.plusDays(3),
      licenceCase = licence,
      now = clock,
    )

    // Then
    assertThat(result).isEqualTo(FUTURE_RELEASES)
  }

  fun aLicenceCaseInformation(
    kind: LicenceKind = LicenceKind.CRD,
    licenceId: Long = 1L,
    versionOfId: Long? = null,
    licenceStatus: LicenceStatus = LicenceStatus.IN_PROGRESS,
    nomisId: String = "A1234AA",
    surname: String? = "One",
    forename: String? = "Person",
    prisonCode: String? = "BAI",
    prisonDescription: String? = "Moorland (HMP)",
    conditionalReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    actualReleaseDate: LocalDate? = LocalDate.of(2021, 10, 22),
    licenceStartDate: LocalDate? = LocalDate.of(2021, 10, 22),
    postRecallReleaseDate: LocalDate? = null,
    updatedByFirstName: String? = "X",
    updatedByLastName: String? = "Y",
    comUsername: String? = "com-user",
    comFirstName: String? = "Com",
    comLastName: String? = "User",
    homeDetentionCurfewActualDate: LocalDate? = null,
  ) = LicenceCase(
    kind = kind,
    licenceId = licenceId,
    versionOfId = versionOfId,
    licenceStatus = licenceStatus,
    prisonNumber = nomisId,
    surname = surname,
    forename = forename,
    prisonCode = prisonCode,
    prisonDescription = prisonDescription,
    conditionalReleaseDate = conditionalReleaseDate,
    actualReleaseDate = actualReleaseDate,
    licenceStartDate = licenceStartDate,
    postRecallReleaseDate = postRecallReleaseDate,
    updatedByFirstName = updatedByFirstName,
    updatedByLastName = updatedByLastName,
    comUsername = comUsername,
    comFirstName = comFirstName,
    comLastName = comLastName,
    homeDetentionCurfewActualDate = homeDetentionCurfewActualDate,
  )
}
