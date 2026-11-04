package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import java.time.LocalDate

class HdcLicenceFactoryTest {

  private val factory = HdcLicenceFactory()

  @Test
  fun `createFromCrd populates HDC dates from NOMIS and preserves CRD fields`() {
    val crdLicence = createCrdLicence()
    val eligibilityDate = LocalDate.of(2026, 1, 1)
    val actualDate = LocalDate.of(2026, 1, 2)
    val endDate = LocalDate.of(2026, 2, 1)
    val prisoner = prisonerSearchResult().copy(
      homeDetentionCurfewEligibilityDate = eligibilityDate,
      homeDetentionCurfewActualDate = actualDate,
      homeDetentionCurfewEndDate = endDate,
    )

    val hdcLicence = factory.createFromCrd(crdLicence, prisoner, IN_PROGRESS)

    assertThat(hdcLicence.homeDetentionCurfewEligibilityDate).isEqualTo(eligibilityDate)
    assertThat(hdcLicence.homeDetentionCurfewActualDate).isEqualTo(actualDate)
    assertThat(hdcLicence.homeDetentionCurfewEndDate).isEqualTo(endDate)
    assertThat(hdcLicence.forename).isEqualTo(crdLicence.forename)
    assertThat(hdcLicence.conditionalReleaseDate).isEqualTo(crdLicence.conditionalReleaseDate)
    assertThat(hdcLicence.statusCode).isEqualTo(IN_PROGRESS)
  }

  @Test
  fun `createFromCrd leaves HDC dates unset when prisoner is unavailable`() {
    val hdcLicence = factory.createFromCrd(createCrdLicence())

    assertThat(hdcLicence.homeDetentionCurfewEligibilityDate).isNull()
    assertThat(hdcLicence.homeDetentionCurfewActualDate).isNull()
    assertThat(hdcLicence.homeDetentionCurfewEndDate).isNull()
    assertThat(hdcLicence.electronicMonitoringProvider).isNull()
  }
}
