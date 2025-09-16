package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.aLicenceSummary
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.caseload.ca.Tabs.determineCaViewCasesTab
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.ATTENTION_NEEDED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.FUTURE_RELEASES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.CaViewCasesTab.RELEASES_IN_NEXT_TWO_WORKING_DAYS
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
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = null,
      licence = aLicenceSummary().copy(licenceStatus = status),
      now = clock,
    )
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns ATTENTION_NEEDED when licence is approved and start date is in the past`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.minusDays(1),
      licence = aLicenceSummary().copy(licenceStatus = APPROVED),
      now = clock,
    )
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `does not return ATTENTION_NEEDED when licence is approved and start date is today`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow,
      licence = aLicenceSummary().copy(licenceStatus = APPROVED),
      now = clock,
    )
    assertThat(result).isNotEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `does not return ATTENTION_NEEDED when licence is approved and start date is tomorrow`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(1),
      licence = aLicenceSummary().copy(licenceStatus = APPROVED),
      now = clock,
    )
    assertThat(result).isNotEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns ATTENTION_NEEDED when no licence and no LSD`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = null,
      licence = null,
      now = clock,
    )
    assertThat(result).isEqualTo(ATTENTION_NEEDED)
  }

  @Test
  fun `returns RELEASES_IN_NEXT_TWO_WORKING_DAYS when licence is LSD is not in past and due to be released in next two days`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(1),
      licence = aLicenceSummary().copy(licenceStatus = APPROVED, isDueToBeReleasedInTheNextTwoWorkingDays = true),
      now = clock,
    )
    assertThat(result).isEqualTo(RELEASES_IN_NEXT_TWO_WORKING_DAYS)
  }

  @Test
  fun `returns RELEASES_IN_NEXT_TWO_WORKING_DAYS when no licence and falls back to CVL info`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = true,
      licenceStartDate = fixedNow.plusDays(2),
      licence = null,
      now = clock,
    )
    assertThat(result).isEqualTo(RELEASES_IN_NEXT_TWO_WORKING_DAYS)
  }

  @Test
  fun `returns FUTURE_RELEASES when licence present`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.plusDays(3),
      licence = aLicenceSummary().copy(isDueToBeReleasedInTheNextTwoWorkingDays = false),
      now = clock,
    )
    assertThat(result).isEqualTo(FUTURE_RELEASES)
  }

  @Test
  fun `returns FUTURE_RELEASES when no licence and falls back to CVL info`() {
    val result = determineCaViewCasesTab(
      isDueToBeReleasedInTheNextTwoWorkingDays = false,
      licenceStartDate = fixedNow.plusDays(3),
      licence = null,
      now = clock,
    )
    assertThat(result).isEqualTo(FUTURE_RELEASES)
  }
}
