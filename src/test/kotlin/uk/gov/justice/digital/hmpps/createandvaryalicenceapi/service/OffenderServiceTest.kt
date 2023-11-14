package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceType
import java.time.LocalDate

class OffenderServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val notifyService = mock<NotifyService>()
  private val releaseDateService = mock<ReleaseDateService>()

  private val service =
    OffenderService(
      licenceRepository,
      auditEventRepository,
      notifyService,
      releaseDateService,
      TEMPLATE_ID,
      maxNumberOfWorkingDaysAllowedToTriggerEmailIfPPIsModifiedOrAllocatedBefore,
    )

  @BeforeEach
  fun reset() {
    reset(licenceRepository, auditEventRepository)
  }

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(releaseDateService.getEarliestReleaseDate(any(), any())).thenReturn(LocalDate.now())
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
          LicenceStatus.ACTIVE,
        ),
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())

    assertThat(auditCaptor.value)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          1L,
          "SYSTEM",
          "SYSTEM",
          "COM updated to X Y on licence for ${aLicenceEntity.forename} ${aLicenceEntity.surname}",
        ),
      )
  }

  @Test
  fun `send licence create email when update offender with new offender manager equal to 5 days to release`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(releaseDateService.getEarliestReleaseDate(any(), any())).thenReturn(LocalDate.now())
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
          LicenceStatus.ACTIVE,
        ),
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, times(1)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `send licence create email when update offender with new offender manager less than 5 days to release`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(releaseDateService.getEarliestReleaseDate(any(), any())).thenReturn(LocalDate.now().minusDays(5))
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
          LicenceStatus.ACTIVE,
        ),
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, times(1)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `don't send licence create email when update offender with new offender manager more than 5 days to release`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(releaseDateService.getEarliestReleaseDate(any(), any())).thenReturn(LocalDate.now().plusDays(6))
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
          LicenceStatus.ACTIVE,
        ),
      )

    verify(licenceRepository, times(1))
      .saveAllAndFlush(expectedUpdatedLicences)

    verify(auditEventRepository, times(1)).saveAndFlush(auditCaptor.capture())
    verify(notifyService, times(0)).sendLicenceCreateEmail(any(), any(), any(), any())
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
      ),
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
          LicenceStatus.ACTIVE,
        ),
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
        probationTeamDescription = "TEAM1 probation team",
      ),
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
          LicenceStatus.ACTIVE,
        ),
      )

    verify(licenceRepository, times(0)).saveAllAndFlush(anyList())
    verify(auditEventRepository, times(0)).saveAndFlush(any())
  }

  @Test
  fun `updates all non-inactive licences for an offender if the offender's personal details have changed`() {
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicenceEntity,
        aLicenceEntity.copy(id = 2, statusCode = LicenceStatus.ACTIVE),
      ),
    )

    val expectedUpdatedLicences = listOf(
      aLicenceEntity.copy(
        forename = "Peter",
        middleNames = "Robin",
        surname = "Smith",
        dateOfBirth = LocalDate.parse("1970-02-01"),
      ),
      aLicenceEntity.copy(
        id = 2,
        statusCode = LicenceStatus.ACTIVE,
        forename = "Peter",
        middleNames = "Robin",
        surname = "Smith",
        dateOfBirth = LocalDate.parse("1970-02-01"),
      ),
    )

    val auditCaptor = ArgumentCaptor.forClass(AuditEvent::class.java)

    service.updateOffenderDetails(aLicenceEntity.nomsId!!, newOffenderDetails)

    verify(licenceRepository, times(1)).findAllByNomsIdAndStatusCodeIn(
      aLicenceEntity.nomsId!!,
      listOf(
        LicenceStatus.IN_PROGRESS,
        LicenceStatus.SUBMITTED,
        LicenceStatus.APPROVED,
        LicenceStatus.VARIATION_IN_PROGRESS,
        LicenceStatus.VARIATION_SUBMITTED,
        LicenceStatus.VARIATION_APPROVED,
        LicenceStatus.VARIATION_REJECTED,
        LicenceStatus.ACTIVE,
      ),
    )

    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditEventRepository, times(2)).saveAndFlush(auditCaptor.capture())
    assertThat(auditCaptor.allValues)
      .extracting("licenceId", "username", "fullName", "summary")
      .isEqualTo(
        listOf(
          Tuple(1L, "SYSTEM", "SYSTEM", "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01"),
          Tuple(2L, "SYSTEM", "SYSTEM", "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01"),
        ),
      )
  }

  private companion object {
    val aLicenceEntity = Licence(
      id = 1L,
      crn = "exampleCrn",
      nomsId = "A1234AB",
      forename = "Robin",
      surname = "Smith",
      dateOfBirth = LocalDate.parse("1970-01-01"),
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
      probationTeamDescription = "TEAM1 probation team",
      actualReleaseDate = LocalDate.parse("2023-11-17"),
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
      probationTeamDescription = "TEAM2 probation team",
    )

    val newOffenderDetails = UpdateOffenderDetailsRequest(
      forename = "Peter",
      middleNames = "Robin",
      surname = "Smith",
      dateOfBirth = LocalDate.parse("1970-02-01"),
    )

    const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"
    const val maxNumberOfWorkingDaysAllowedToTriggerEmailIfPPIsModifiedOrAllocatedBefore = 5
  }
}
