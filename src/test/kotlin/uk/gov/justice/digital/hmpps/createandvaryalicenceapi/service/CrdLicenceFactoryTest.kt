package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.ProbationContact
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createHdcLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.APPROVED
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.IN_PROGRESS

class CrdLicenceFactoryTest {

  private val factory = CrdLicenceFactory()

  @Test
  fun `creates a new CRD licence from an HDC licence`() {
    // Given
    val hdcLicence = createHdcLicence(id = 123).copy(
      statusCode = APPROVED,
      probationContact = ProbationContact(person = "Joe Bloggs"),
    )

    // When
    val crdLicence = factory.createFromHdc(hdcLicence, IN_PROGRESS)

    // Then
    assertThat(crdLicence).isInstanceOf(CrdLicence::class.java)
    assertThat(crdLicence.id).isEqualTo(-1)
    assertThat(crdLicence.kind).isEqualTo(LicenceKind.CRD)
    assertThat(crdLicence.eligibleKind).isEqualTo(EligibleKind.CRD)
    assertThat(crdLicence.statusCode).isEqualTo(IN_PROGRESS)
    assertThat(crdLicence.versionOfId).isEqualTo(hdcLicence.id)
    assertThat(crdLicence.probationContact).isNull()
    assertThat(crdLicence.nomsId).isEqualTo(hdcLicence.nomsId)
    assertThat(crdLicence.createdBy).isEqualTo(hdcLicence.createdBy)
    assertThat(crdLicence.responsibleCom).isEqualTo(hdcLicence.responsibleCom)
  }
}
