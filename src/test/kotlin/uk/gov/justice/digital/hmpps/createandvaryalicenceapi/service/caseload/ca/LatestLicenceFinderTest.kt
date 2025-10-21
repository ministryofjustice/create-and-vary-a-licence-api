package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.prison.LatestLicenceFinder.findLatestLicenceCases
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LatestLicenceFinderTest {

  @Test
  fun `should return the first element if the licences length is one`() {
    // Given
    val licenceCases = createLicenceCase(licenceStatus = LicenceStatus.APPROVED)

    // When
    val result = findLatestLicenceCases(listOf(licenceCases))

    // Then
    assertThat(result).isEqualTo(licenceCases)
  }

  @Test
  fun `should return the IN_PROGRESS licence if there are IN_PROGRESS and TIMED_OUT licences`() {
    // Given
    val licenceCases = listOf(
      createLicenceCase(licenceStatus = LicenceStatus.IN_PROGRESS),
      createLicenceCase(licenceStatus = LicenceStatus.TIMED_OUT),
    )

    // When
    val result = findLatestLicenceCases(licenceCases)

    // Then
    assertThat(result).isEqualTo(licenceCases.first())
  }

  @Test
  fun `should return the SUBMITTED licence if there are IN_PROGRESS and SUBMITTED licences`() {
    // Given
    val licenceCases = listOf(
      createLicenceCase(licenceStatus = LicenceStatus.SUBMITTED),
      createLicenceCase(licenceStatus = LicenceStatus.IN_PROGRESS),
    )

    // When
    val result = findLatestLicenceCases(licenceCases)

    // Then
    assertThat(result).isEqualTo(licenceCases.first())
  }
}
