import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.CvlRecordService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.EligibilityService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anEligibilityAssessment
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.prisonerSearchResult
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class LicenceTypeTest {
  private val eligibilityService = mock<EligibilityService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service = CvlRecordService(eligibilityService, releaseDateService)

  @BeforeEach
  fun reset() {
    whenever(eligibilityService.getEligibilityAssessments(any(), any())).thenReturn(
      mapOf(prisonerSearchResult().prisonerNumber to anEligibilityAssessment()),
    )
  }

  @Test
  fun `should default to AP`() {
    val nomisRecord =
      prisonerSearchResult().copy(licenceExpiryDate = null, topupSupervisionExpiryDate = null)
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `should be PSS when TUSED is defined and LED is undefined`() {
    val nomisRecord = prisonerSearchResult()
      .copy(licenceExpiryDate = null, topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22))
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.PSS)
  }

  @Test
  fun `should be AP when LED is defined and TUSED is undefined`() {
    val nomisRecord = prisonerSearchResult()
      .copy(licenceExpiryDate = LocalDate.of(2021, 10, 22), topupSupervisionExpiryDate = null)
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `should be AP when TUSED is before LED`() {
    val nomisRecord = prisonerSearchResult().copy(
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 21),
    )
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `should be AP when TUSED is equal to LED`() {
    val nomisRecord = prisonerSearchResult().copy(
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
    )
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP)
  }

  @Test
  fun `should be AP_PSS when TUSED is after LED`() {
    val nomisRecord = prisonerSearchResult().copy(
      licenceExpiryDate = LocalDate.of(2021, 10, 22),
      topupSupervisionExpiryDate = LocalDate.of(2021, 10, 23),
    )
    val cvlRecord = service.getCvlRecord(nomisRecord, "AREA1")
    assertThat(cvlRecord.licenceType).isEqualTo(LicenceType.AP_PSS)
  }
}
