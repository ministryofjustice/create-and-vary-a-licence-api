package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceCase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.LatestLicenceFinder.findLatestLicenceCases
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import java.time.LocalDate

class LatestLicenceFinderTest {

  @Test
  fun `should return the first element if the licences length is one`() {
    // Given
    val licences = aLicenceCaseInformation(licenceStatus = LicenceStatus.APPROVED)

    // When
    val result = findLatestLicenceCases(listOf(licences))

    // Then
    assertThat(result).isEqualTo(licences)
  }

  @Test
  fun `should return the IN_PROGRESS licence if there are IN_PROGRESS and TIMED_OUT licences`() {
    // Given
    val licences = listOf(
      aLicenceCaseInformation(licenceStatus = LicenceStatus.IN_PROGRESS),
      aLicenceCaseInformation(licenceStatus = LicenceStatus.TIMED_OUT),
    )

    // When
    val result = findLatestLicenceCases(licences)

    // Then
    assertThat(result).isEqualTo(licences.first())
  }

  @Test
  fun `should return the SUBMITTED licence if there are IN_PROGRESS and SUBMITTED licences`() {
    // Given
    val licences = listOf(
      aLicenceCaseInformation(licenceStatus = LicenceStatus.SUBMITTED),
      aLicenceCaseInformation(licenceStatus = LicenceStatus.IN_PROGRESS),
    )

    // When
    val result = findLatestLicenceCases(licences)

    // Then
    assertThat(result).isEqualTo(licences.first())
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
