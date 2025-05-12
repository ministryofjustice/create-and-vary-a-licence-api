package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.ACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceFunctionsTest {

  private val fiveDaysAgo = LocalDate.now().minusDays(5)
  private val tomorrow = LocalDate.now().plusDays(1)

  private val testCrdLicence = TestData.createCrdLicence().copy(
    statusCode = APPROVED,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testSentenceChanges = SentenceDates(
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    homeDetentionCurfewActualDate = fiveDaysAgo,
    homeDetentionCurfewEndDate = fiveDaysAgo,
  )

  @Test
  fun `Sentence ARD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(actualReleaseDate = tomorrow)))
      .isEqualTo(INACTIVE)
  }

  @Test
  fun `Sentence CRD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(INACTIVE)
  }

  @Test
  fun `Sentence CRD change for IN_PROGRESS licence returns current status`() {
    val licence = testCrdLicence.copy(statusCode = IN_PROGRESS)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(IN_PROGRESS)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP_PSS licence returns INACTIVE status `() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP licence returns INACTIVE status `() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE, typeCode = LicenceType.AP)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE PSS licence returns current status `() {
    val licence = testCrdLicence.copy(statusCode = ACTIVE, typeCode = LicenceType.PSS)
    assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(ACTIVE)
  }
}
