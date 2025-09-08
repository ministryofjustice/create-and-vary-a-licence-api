package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.com
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import java.time.LocalDate

class OffenderServiceTest {
  private val licenceRepository = mock<LicenceRepository>()
  private val auditEventRepository = mock<AuditEventRepository>()
  private val auditService = mock<AuditService>()
  private val notifyService = mock<NotifyService>()
  private val releaseDateService = mock<ReleaseDateService>()
  private val staffRepository = mock<StaffRepository>()

  private val service = OffenderService(
    licenceRepository,
    auditEventRepository,
    auditService,
    notifyService,
    releaseDateService,
    staffRepository,
    TEMPLATE_ID,
  )

  @BeforeEach
  fun reset() = reset(licenceRepository, auditEventRepository)

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    val user = com()
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(user)

    val expectedUpdatedLicences = listOf(aLicenceEntity.copy(responsibleCom = comDetails))

    service.updateOffenderWithResponsibleCom("exampleCrn", existingComDetails, comDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditService).recordAuditEventComUpdated(
      eq(expectedUpdatedLicences[0]),
      eq(existingComDetails),
      eq(comDetails),
      eq(user),
    )
  }

  @Test
  fun `send licence create email when isLateAllocationWarningRequired returns true`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(true)
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(existingComDetails)
    val user = com()
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(user)
    val expectedUpdatedLicences = listOf(aLicenceEntity.copy(responsibleCom = comDetails))

    service.updateOffenderWithResponsibleCom("exampleCrn", existingComDetails, comDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)

    verify(auditService).recordAuditEventComUpdated(
      eq(expectedUpdatedLicences[0]),
      eq(existingComDetails),
      eq(comDetails),
      eq(user),
    )
    verify(notifyService, times(1)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `don't send licence create email when isLateAllocationWarningRequired returns false`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(
          actualReleaseDate = LocalDate.parse("2023-11-20"),
        ),
      ),
    )
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(false)
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(existingComDetails)
    val expectedUpdatedLicences =
      listOf(aLicenceEntity.copy(actualReleaseDate = LocalDate.parse("2023-11-20"), responsibleCom = comDetails))

    service.updateOffenderWithResponsibleCom("exampleCrn", existingComDetails, comDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditService).recordAuditEventComUpdated(any(), any(), eq(comDetails), any())
    verify(notifyService, times(0)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `don't send licence create email when licence status is neither NOT_STARTED or IN_PROGRESS even isLateAllocationWarningRequired returns true`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(
          statusCode = LicenceStatus.SUBMITTED,
          actualReleaseDate = LocalDate.parse("2023-11-14"),
        ),
      ),
    )
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(false)
    val user = com()
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(user)
    val expectedUpdatedLicences = listOf(
      aLicenceEntity.copy(
        statusCode = LicenceStatus.SUBMITTED,
        actualReleaseDate = LocalDate.parse("2023-11-14"),
        responsibleCom = comDetails,
      ),
    )

    service.updateOffenderWithResponsibleCom("exampleCrn", existingComDetails, comDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditService).recordAuditEventComUpdated(any(), any(), eq(comDetails), eq(user))
    verify(notifyService, times(0)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `updates all in-flight licences associated with an offender with new probation region`() {
    val user = com()
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(aLicenceEntity))
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(user)
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

    service.updateProbationTeam("exampleCrn", newProbationRegionDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)
    verify(auditService).recordAuditEventProbationTeamUpdated(eq(aLicenceEntity), any(), eq(user))
  }

  @Test
  fun `does not update licences with probation region if it has not changed`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        aLicenceEntity.copy(
          probationAreaCode = "N01",
          probationAreaDescription = "N01 Region",
          probationPduCode = "PDU1",
          probationPduDescription = "PDU1 Pdu",
          probationLauCode = "LAU1",
          probationLauDescription = "LAU1 Lau",
          probationTeamCode = "TEAM1",
          probationTeamDescription = "TEAM1 probation team",
        ),
      ),
    )

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

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
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

    service.updateOffenderDetails(aLicenceEntity.nomsId!!, newOffenderDetails)

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(aLicenceEntity.nomsId!!, IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)

    argumentCaptor<List<AuditEvent>>().apply {
      verify(auditEventRepository).saveAllAndFlush(capture())
      assertThat(firstValue).extracting("licenceId", "username", "fullName", "summary").isEqualTo(
        listOf(
          Tuple(
            1L,
            "SYSTEM",
            "SYSTEM",
            "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01",
          ),
          Tuple(
            2L,
            "SYSTEM",
            "SYSTEM",
            "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01",
          ),
        ),
      )
    }
  }

  private companion object {
    val aLicenceEntity = TestData.createCrdLicence().copy()

    val existingComDetails = CommunityOffenderManager(
      staffIdentifier = 1000,
      username = "username",
      email = "user@probation.gov.uk",
      firstName = "A",
      lastName = "B",
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
  }
}
