package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.LatestLicenceFinder.findLatestLicenceCases
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

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
}
