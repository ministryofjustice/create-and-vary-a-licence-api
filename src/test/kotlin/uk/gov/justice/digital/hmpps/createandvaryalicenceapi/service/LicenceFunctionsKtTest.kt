package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateSentenceDatesRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceFunctionsKtTest {

  private val fiveDaysAgo = LocalDate.now().minusDays(5)
  private val fourDaysAgo = LocalDate.now().minusDays(4)
  private val tomorrow = LocalDate.now().plusDays(1)

  private val testLicence = Licence(
    statusCode = LicenceStatus.APPROVED,
    kind = LicenceKind.CRD,
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
    typeCode = LicenceType.AP_PSS,
  )

  private val testSentenceChanges = UpdateSentenceDatesRequest(
    licenceStartDate = fiveDaysAgo,
    licenceExpiryDate = fiveDaysAgo,
    sentenceEndDate = fiveDaysAgo,
    topupSupervisionStartDate = fiveDaysAgo,
    topupSupervisionExpiryDate = fiveDaysAgo,
  )

  @Test
  fun `Sentence Changes should return for material change for LED update`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE)

    Assertions.assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(licenceExpiryDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = true,
        sedChanged = false,
        tussdChanged = false,
        tusedChanged = false,
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return no material change for SED when licence is not approved`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.IN_PROGRESS)

    Assertions.assertThat(
      licence.getSentenceChanges(testSentenceChanges.copy(sentenceEndDate = fourDaysAgo)),
    ).isEqualTo(
      SentenceChanges(
        lsdChanged = false,
        ledChanged = false,
        sedChanged = true,
        tussdChanged = false,
        tusedChanged = false,
        isMaterial = false,
      ),
    )
  }

  @Test
  fun `Sentence Changes should return material change for SED when licence is approved`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.APPROVED)

    Assertions.assertThat(
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
        isMaterial = true,
      ),
    )
  }

  @Test
  fun `Sentence ARD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(actualReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence CRD change for ACTIVE licence returns new INACTIVE status`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence CRD change for IN_PROGRESS licence returns current status`() {
    val licence = testLicence.copy(statusCode = LicenceStatus.IN_PROGRESS)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(conditionalReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.IN_PROGRESS)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP_PSS licence returns INACTIVE status `() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE AP licence returns INACTIVE status `() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE, typeCode = LicenceType.AP)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.INACTIVE)
  }

  @Test
  fun `Sentence PPRD change for ACTIVE PSS licence returns current status `() {
    val licence = testLicence.copy(statusCode = LicenceStatus.ACTIVE, typeCode = LicenceType.PSS)
    Assertions.assertThat(licence.calculateStatusCode(testSentenceChanges.copy(postRecallReleaseDate = tomorrow)))
      .isEqualTo(LicenceStatus.ACTIVE)
  }
}
