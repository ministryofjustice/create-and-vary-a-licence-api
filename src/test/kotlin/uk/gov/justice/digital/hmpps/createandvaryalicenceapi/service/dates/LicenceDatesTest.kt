package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.HDCAD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.HDCENDDATE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.LED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.LSD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.PRRD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.LicenceDateType.SED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceDatesTest {

  private val fiveDaysAgo = LocalDate.now().minusDays(5)
  private val fourDaysAgo = LocalDate.now().minusDays(4)

  private val testCrdLicence = TestData.createCrdLicence().copy(
    statusCode = APPROVED,
    conditionalReleaseDate = fiveDaysAgo,
    actualReleaseDate = fiveDaysAgo,
    sentenceStartDate = fiveDaysAgo,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testHdcLicence = TestData.createHdcLicence().copy(
    statusCode = APPROVED,
    conditionalReleaseDate = fiveDaysAgo,
    actualReleaseDate = fiveDaysAgo,
    sentenceStartDate = fiveDaysAgo,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    homeDetentionCurfewActualDate = fiveDaysAgo,
    homeDetentionCurfewEndDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testSentenceChanges = SentenceDates(
    conditionalReleaseDate = fiveDaysAgo,
    actualReleaseDate = fiveDaysAgo,
    sentenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    homeDetentionCurfewActualDate = fiveDaysAgo,
    homeDetentionCurfewEndDate = fiveDaysAgo,
  )

  @Test
  fun `Sentence Changes should return for material change for LED update`() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE)

    val sentenceChanges =
      licence.getDateChanges(testSentenceChanges.copy(licenceExpiryDate = fourDaysAgo), newLsd = fiveDaysAgo)

    assertThat(sentenceChanges.changedTypes()).containsExactly(LED)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  @Test
  fun `Sentence Changes should return for material change for LSD update`() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE, licenceStartDate = fiveDaysAgo)

    val sentenceChanges = licence.getDateChanges(testSentenceChanges, newLsd = fourDaysAgo)

    assertThat(sentenceChanges.changedTypes()).containsExactly(LSD)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  @Test
  fun `Sentence Changes should return for material change for HDCAD update to HDC licence`() {
    val licence = testHdcLicence.copy(homeDetentionCurfewActualDate = fiveDaysAgo)

    val sentenceChanges = licence.getDateChanges(
      testSentenceChanges.copy(homeDetentionCurfewActualDate = fourDaysAgo),
      newLsd = fiveDaysAgo,
    )

    assertThat(sentenceChanges.changedTypes()).containsExactly(HDCAD)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  @Test
  fun `Sentence Changes should return for material change for HDC end date update to HDC licence`() {
    val licence = testHdcLicence.copy(homeDetentionCurfewEndDate = fiveDaysAgo)

    val sentenceChanges = licence.getDateChanges(
      testSentenceChanges.copy(homeDetentionCurfewEndDate = fourDaysAgo),
      newLsd = fiveDaysAgo,
    )

    assertThat(sentenceChanges.changedTypes()).containsExactly(HDCENDDATE)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  @Test
  fun `Sentence Changes should return no material change for SED when licence is not approved`() {
    val licence = testCrdLicence.copy(statusCode = IN_PROGRESS)

    val sentenceChanges =
      licence.getDateChanges(testSentenceChanges.copy(sentenceEndDate = fourDaysAgo), newLsd = fiveDaysAgo)

    assertThat(sentenceChanges.changedTypes()).containsExactly(SED)
    assertThat(sentenceChanges.isMaterial).isFalse
  }

  @Test
  fun `Sentence Changes should return material change for SED when licence is approved`() {
    val licence = testCrdLicence.copy(statusCode = APPROVED)

    val sentenceChanges = licence.getDateChanges(
      testSentenceChanges.copy(sentenceEndDate = fourDaysAgo),
      LocalDate.now().minusDays(5),
    )

    assertThat(sentenceChanges.changedTypes()).containsExactly(SED)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  @Test
  fun `Sentence Changes should return material change for PPRD update`() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE)

    val sentenceChanges =
      licence.getDateChanges(testSentenceChanges.copy(postRecallReleaseDate = fourDaysAgo), newLsd = fiveDaysAgo)

    assertThat(sentenceChanges.changedTypes()).containsExactly(PRRD)
    assertThat(sentenceChanges.isMaterial).isTrue
  }

  private fun DateChanges.changedTypes(): List<LicenceDateType> = this.filter { it.changed }.map { it.type }
}
