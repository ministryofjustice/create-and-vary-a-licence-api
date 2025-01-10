package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceFunctionsKtTest {

  private val fiveDaysAgo = LocalDate.now().minusDays(5)
  private val fourDaysAgo = LocalDate.now().minusDays(4)
  private val tomorrow = LocalDate.now().plusDays(1)

  private val testCrdLicence = TestData.createCrdLicence().copy(
    statusCode = LicenceStatus.APPROVED,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testHdcLicence = TestData.createHdcLicence().copy(
    statusCode = LicenceStatus.APPROVED,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    homeDetentionCurfewActualDate = fiveDaysAgo,
    homeDetentionCurfewEndDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testSentenceChanges = UpdateSentenceDatesRequest(
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    homeDetentionCurfewActualDate = fiveDaysAgo,
    homeDetentionCurfewEndDate = fiveDaysAgo,
  )

  @Test
  fun `Sentence Changes should return for material change for LED update`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE)

    assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(licenceExpiryDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = true,
        sedChanged = false,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = false,
        hdcadChanged = false,
        hdcEndDateChanged = false,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return for material change for HDCAD update to HDC licence`() {
    val licence = testHdcLicence.copy(homeDetentionCurfewActualDate = fiveDaysAgo)

    assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(homeDetentionCurfewActualDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = false,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = false,
        hdcadChanged = true,
        hdcEndDateChanged = false,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return for material change for HDC end date update to HDC licence`() {
    val licence = testHdcLicence.copy(homeDetentionCurfewEndDate = fiveDaysAgo)

    assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(homeDetentionCurfewEndDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = false,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = false,
        hdcadChanged = false,
        hdcEndDateChanged = true,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return no material change for SED when licence is not approved`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.IN_PROGRESS)

    assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(sentenceEndDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = true,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = false,
        hdcadChanged = false,
        hdcEndDateChanged = false,
        isMaterial = false,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return material change for PPRD update`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE)

    assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(postRecallReleaseDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = false,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = true,
        hdcadChanged = false,
        hdcEndDateChanged = false,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return material change for SED when licence is approved`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.APPROVED)

    assertThat(
      licence.getSentenceChanges(
        testSentenceChanges.copy(sentenceEndDate = fourDaysAgo),
      ),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = true,
        tussdChanged = false,
        tusedChanged = false,
        prrdChanged = false,
        hdcadChanged = false,
        hdcEndDateChanged = false,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence ARD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(actualReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence CRD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence CRD change for IN_PROGRESS licence returns current status`() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.IN_PROGRESS)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.IN_PROGRESS)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP_PSS licence returns INACTIVE status `() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP licence returns INACTIVE status `() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE, typeCode = LicenceType.AP)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE PSS licence returns current status `() {
    val licence = testCrdLicence.copy(statusCode = LicenceStatus.ACTIVE, typeCode = LicenceType.PSS)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.ACTIVE)
  }
}
