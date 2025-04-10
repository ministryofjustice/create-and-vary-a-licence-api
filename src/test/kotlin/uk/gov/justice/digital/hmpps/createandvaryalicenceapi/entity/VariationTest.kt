import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence.Companion.SYSTEM_USER
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.com
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcVariationLicence
import java.time.LocalDate

class VariationTest {

  @Test
  fun `recordUpdate tracks lastUpdatedDate and staff member responsible`() {
    val testLicence = createHdcVariationLicence()
    val com = com().copy(username = "TestCom")
    testLicence.recordUpdate(com)

    assertThat(testLicence.dateLastUpdated!!.toLocalDate()).isEqualTo(LocalDate.now())
    assertThat(testLicence.updatedByUsername).isEqualTo(com.username)
    assertThat(testLicence.updatedBy).isEqualTo(com)
  }

  @Test
  fun `recordUpdate defaults to SYSTEM_USER if no staff member is passed in`() {
    val testLicence = createHdcVariationLicence().copy(updatedBy = com())
    testLicence.recordUpdate(null)

    assertThat(testLicence.dateLastUpdated!!.toLocalDate()).isEqualTo(LocalDate.now())
    assertThat(testLicence.updatedByUsername).isEqualTo(SYSTEM_USER)
    assertThat(testLicence.updatedBy).isEqualTo(com())
  }

  @Test
  fun `updateSpoDiscussion updates the licence correctly and calls recordUpdate`() {
    val testLicence = createHdcVariationLicence()
    val com = com().copy(username = "TestCom")
    testLicence.updateSpoDiscussion("Yes", com)

    assertThat(testLicence.spoDiscussion).isEqualTo("Yes")
    assertThat(testLicence.updatedByUsername).isEqualTo(com.username)
  }

  @Test
  fun `updateVloDiscussion updates the licence correctly and calls recordUpdate`() {
    val testLicence = createHdcVariationLicence()
    val com = com().copy(username = "TestCom")
    testLicence.updateVloDiscussion("Yes", com)

    assertThat(testLicence.vloDiscussion).isEqualTo("Yes")
    assertThat(testLicence.updatedByUsername).isEqualTo(com.username)
  }
}
