package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.Licence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
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
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.APPROVED,
          LicenceStatus.VARIATION_IN_PROGRESS,
          LicenceStatus.VARIATION_SUBMITTED,
          LicenceStatus.VARIATION_APPROVED,
          LicenceStatus.VARIATION_REJECTED,
          LicenceStatus.ACTIVE
        )
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "COM updated to X Y on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `updates all in-flight licences associated with an offender with new probation region`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    val expectedUpdatedLicences = listOf(
      aLicenceEntity.copy(
        probationAreaCode = "N02",
        probationAreaDescription = "N02 Region",
        probationPduCode = "PDU2",
        probationPduDescription = "PDU2 Pdu",
        probationLauCode = "LAU2",
        probationLauDescription = "LAU2 Lau",
        probationTeamCode = "TEAM2",
        probationTeamDescription = "TEAM2 probation team",
      )
    )

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.updateProbationTeam("exampleCrn", newProbationRegionDetails)

    verify(licenceRepository, times(1))
      .findAllByCrnAndStatusCodeIn(
        "exampleCrn",
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.APPROVED,
          LicenceStatus.VARIATION_IN_PROGRESS,
          LicenceStatus.VARIATION_SUBMITTED,
          LicenceStatus.VARIATION_APPROVED,
          LicenceStatus.VARIATION_REJECTED,
          LicenceStatus.ACTIVE
        )
      )

    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(listOf(1L, "SYSTEM", "SYSTEM", "Probation team updated to TEAM2 probation team at N02 Region on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}"))
  }

  @Test
  fun `does not update licences with probation region if it has not changed`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))

    service.updateProbationTeam(
      "exampleCrn",
      UpdateProbationTeamRequest(
        probationAreaCode = "N01",
        probationAreaDescription = "N01 Region",
        probationPduCode = "PDU1",
        probationPduDescription = "PDU1 Pdu",
        probationLauCode = "LAU1",
        probationLauDescription = "LAU1 Lau",
        probationTeamCode = "TEAM1",
        probationTeamDescription = "TEAM1 probation team"
      )
    )

    verify(licenceRepository, times(1))
      .findAllByCrnAndStatusCodeIn(
        "exampleCrn",
        listOf(
          LicenceStatus.IN_PROGRESS,
          LicenceStatus.SUBMITTED,
          LicenceStatus.APPROVED,
          LicenceStatus.VARIATION_IN_PROGRESS,
          LicenceStatus.VARIATION_SUBMITTED,
          LicenceStatus.VARIATION_APPROVED,
          LicenceStatus.VARIATION_REJECTED,
          LicenceStatus.ACTIVE
        )
      )

    verify(licenceRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
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
      probationAreaCode = "N01",
      probationAreaDescription = "N01 Region",
      probationPduCode = "PDU1",
      probationPduDescription = "PDU1 Pdu",
      probationLauCode = "LAU1",
      probationLauDescription = "LAU1 Lau",
      probationTeamCode = "TEAM1",
      probationTeamDescription = "TEAM1 probation team"
    )

    val comDetails = CommunityOffenderManager(
      staffIdentifier = 2000,
      username = "joebloggs",
      email = "jbloggs@probation.gov.uk",
      firstName = "X",
      lastName = "Y",
    )

    val newProbationRegionDetails = UpdateProbationTeamRequest(
      probationAreaCode = "N02",
      probationAreaDescription = "N02 Region",
      probationPduCode = "PDU2",
      probationPduDescription = "PDU2 Pdu",
      probationLauCode = "LAU2",
      probationLauDescription = "LAU2 Lau",
      probationTeamCode = "TEAM2",
      probationTeamDescription = "TEAM2 probation team"
    )
  }
}
