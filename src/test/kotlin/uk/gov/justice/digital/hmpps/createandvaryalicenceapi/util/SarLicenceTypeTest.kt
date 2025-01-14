import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.Prisoner
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class SarLicenceTypeTest {
  val companion = LicenceType.Companion

  @Nested
  inner class PrisonerSearchPrisonerParam {
    @Test
    fun `should default to AP`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(licenceExpiryDate = null, topupSupervisionExpiryDate = null)
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be PSS when TUSED is defined and LED is undefined`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(licenceExpiryDate = null, topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22))
      assert(companion.getLicenceType(nomisRecord) == LicenceType.PSS)
    }

    @Test
    fun `should be AP when LED is defined and TUSED is undefined`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(licenceExpiryDate = LocalDate.of(2021, 10, 22), topupSupervisionExpiryDate = null)
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP when TUSED is before LED`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 21),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP when TUSED is equal to LED`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP_PSS when TUSED is after LED`() {
      val nomisRecord = TestData.prisonerSearchResult().copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 23),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP_PSS)
    }
  }

  @Nested
  inner class PrisonerParam {
    val prisoner = Prisoner()

    @Test
    fun `should default to AP`() {
      val nomisRecord = prisoner.copy(licenceExpiryDate = null, topupSupervisionExpiryDate = null)
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be PSS when TUSED is defined and LED is undefined`() {
      val nomisRecord = prisoner.copy(licenceExpiryDate = null, topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22))
      assert(companion.getLicenceType(nomisRecord) == LicenceType.PSS)
    }

    @Test
    fun `should be AP when LED is defined and TUSED is undefined`() {
      val nomisRecord = prisoner.copy(licenceExpiryDate = LocalDate.of(2021, 10, 22), topupSupervisionExpiryDate = null)
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP when TUSED is before LED`() {
      val nomisRecord = prisoner.copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 21),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP when TUSED is equal to LED`() {
      val nomisRecord = prisoner.copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 22),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP)
    }

    @Test
    fun `should be AP_PSS when TUSED is after LED`() {
      val nomisRecord = prisoner.copy(
        licenceExpiryDate = LocalDate.of(2021, 10, 22),
        topupSupervisionExpiryDate = LocalDate.of(2021, 10, 23),
      )
      assert(companion.getLicenceType(nomisRecord) == LicenceType.AP_PSS)
    }
  }
}
