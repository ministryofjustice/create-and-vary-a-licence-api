package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType

class OffenderServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()

  private val service = OffenderService(licenceRepository, auditEventRepository)

  @BeforeEach
  fun reset() {
    reset(licenceRepository, auditEventRepository)
  }

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    val expectedUpdatedLicences = listOf(aLicenceEntity.copy(responsibleCom = comDetails))

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.updateOffenderWithResponsibleCom("exampleCrn", comDetails)

    verify(licenceRepository, times(1))
      .findAllByCrnAndStatusCodeIn(
        "exampleCrn",
        listOf(LicenceStatus.IN_PROGRESS, LicenceStatus.SUBMITTED, LicenceStatus.APPROVED, LicenceStatus.ACTIVE)
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "COM updated to X Y on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  private companion object {
    val aLicenceEntity = Licence(
      id = 1L,
      crn = "exampleCrn",
      forename = "Robin",
      surname = "Smith",
      typeCode = LicenceType.AP,
      statusCode = LicenceStatus.IN_PROGRESS,
      version = "1.0",
    )

    val comDetails = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )
  }
}
