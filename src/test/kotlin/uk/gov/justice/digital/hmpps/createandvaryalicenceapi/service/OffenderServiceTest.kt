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
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateOffenderDetailsRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.AuditEventRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.anotherCommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createCrdLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createTimeServedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.createVariationLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.dates.ReleaseDateService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.Companion.IN_FLIGHT_LICENCES
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.SUBMITTED
import java.time.LocalDate
import kotlin.toString

const val TEMPLATE_ID = "xxx-xxx-xxx-xxx"

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
  fun reset() = reset(licenceRepository, auditEventRepository, auditService, notifyService, releaseDateService, staffRepository)

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    val originalCom = communityOffenderManager()
    val anotherCom = anotherCommunityOffenderManager()

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(licenceWithOriginalCom),
    )
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(anotherCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", originalCom, anotherCom)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(licenceWithNewCom))
    verify(auditService).recordAuditEventComUpdated(
      eq(licenceWithNewCom),
      eq(originalCom),
      eq(anotherCom),
      eq(anotherCom),
    )
  }

  @Test
  fun `send licence create email when isLateAllocationWarningRequired returns true`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(licenceWithOriginalCom))
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(true)

    whenever(staffRepository.findByUsernameIgnoreCase(originalCom.username)).thenReturn(originalCom)
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", originalCom, newCom)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(licenceWithNewCom))
    verify(auditService).recordAuditEventComUpdated(eq(licenceWithNewCom), eq(originalCom), eq(newCom), any())
    verify(notifyService, times(1)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `don't send licence create email when isLateAllocationWarningRequired returns false`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        licenceWithOriginalCom.copy(
          actualReleaseDate = LocalDate.parse("2023-11-20"),
        ),
      ),
    )
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(false)
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", originalCom, newCom)

    val newLicence = licenceWithNewCom.copy(actualReleaseDate = LocalDate.parse("2023-11-20"))
    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(newLicence))
    verify(auditService).recordAuditEventComUpdated(newLicence, originalCom, newCom, null)
    verify(notifyService, times(0)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `don't send licence create email when licence status is neither NOT_STARTED or IN_PROGRESS even isLateAllocationWarningRequired returns true`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        licenceWithOriginalCom.copy(
          statusCode = SUBMITTED,
          actualReleaseDate = LocalDate.parse("2023-11-14"),
        ),
      ),
    )
    whenever(releaseDateService.isLateAllocationWarningRequired(any())).thenReturn(false)
    whenever(staffRepository.findByUsernameIgnoreCase(originalCom.username)).thenReturn(originalCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", originalCom, newCom)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)

    val updatedLicence =
      licenceWithNewCom.copy(statusCode = SUBMITTED, actualReleaseDate = LocalDate.parse("2023-11-14"))
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(updatedLicence))
    verify(auditService).recordAuditEventComUpdated(eq(updatedLicence), eq(originalCom), eq(newCom), any())
    verify(notifyService, times(0)).sendLicenceCreateEmail(any(), any(), any(), any())
  }

  @Test
  fun `updates all in-flight licences associated with an offender with new probation region`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(licenceWithOriginalCom))
    whenever(staffRepository.findByUsernameIgnoreCase(originalCom.username)).thenReturn(originalCom)

    service.updateProbationTeam("exampleCrn", newProbationRegionDetails)

    verify(licenceRepository, times(1)).findAllByCrnAndStatusCodeIn("exampleCrn", IN_FLIGHT_LICENCES)
    val updatedLicence = licenceWithOriginalCom.copy(
      probationAreaCode = "N02",
      probationAreaDescription = "N02 Region",
      probationPduCode = "PDU2",
      probationPduDescription = "PDU2 Pdu",
      probationLauCode = "LAU2",
      probationLauDescription = "LAU2 Lau",
      probationTeamCode = "TEAM2",
      probationTeamDescription = "TEAM2 probation team",
    )
    verify(licenceRepository, times(1)).saveAllAndFlush(listOf(updatedLicence))
    verify(auditService).recordAuditEventProbationTeamUpdated(eq(updatedLicence), eq(newProbationRegionDetails), any())
  }

  @Test
  fun `does not update licences with probation region if it has not changed`() {
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        licenceWithOriginalCom.copy(
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
        licenceWithOriginalCom,
        licenceWithOriginalCom.copy(id = 2, statusCode = LicenceStatus.ACTIVE),
      ),
    )

    val expectedUpdatedLicences = listOf(
      licenceWithOriginalCom.copy(
        forename = "Peter",
        middleNames = "Robin",
        surname = "Smith",
        dateOfBirth = LocalDate.parse("1970-02-01"),
      ),
      licenceWithOriginalCom.copy(
        id = 2,
        statusCode = LicenceStatus.ACTIVE,
        forename = "Peter",
        middleNames = "Robin",
        surname = "Smith",
        dateOfBirth = LocalDate.parse("1970-02-01"),
      ),
    )

    service.updateOffenderDetails(licenceWithOriginalCom.nomsId!!, newOffenderDetails)

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(licenceWithOriginalCom.nomsId!!, IN_FLIGHT_LICENCES)
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

  @Test
  fun `sends initial COM allocation email when time served licence exists and no previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence().copy(
      responsibleCom = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(timeServedLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", null, newCom)

    verify(notifyService, times(1)).sendInitialComAllocationEmail(
      emailAddress = newCom.email!!,
      comName = "${newCom.firstName} ${newCom.lastName}",
      offenderName = "${timeServedLicence.forename} ${timeServedLicence.surname}",
      crn = timeServedLicence.crn!!,
      licenceId = timeServedLicence.id.toString(),
    )
  }

  @Test
  fun `sends initial COM allocation email when variation licence exists and no previous COM was allocated`() {
    val variationLicence = createVariationLicence().copy(
      responsibleCom = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(variationLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", null, newCom)

    verify(notifyService, times(1)).sendInitialComAllocationEmail(
      emailAddress = newCom.email!!,
      comName = "${newCom.firstName} ${newCom.lastName}",
      offenderName = "${variationLicence.forename} ${variationLicence.surname}",
      crn = variationLicence.crn!!,
      licenceId = variationLicence.id.toString(),
    )
  }

  @Test
  fun `does not send initial COM allocation email when time served licence exists and previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence()

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(timeServedLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(originalCom.username)).thenReturn(originalCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", originalCom, newCom)

    verifyNoInteractions(notifyService)
  }

  @Test
  fun `does not send initial COM allocation email when no time served licence or variation licence exists`() {
    val crdLicence = createCrdLicence()

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateOffenderWithResponsibleCom("exampleCrn", null, newCom)

    verifyNoInteractions(notifyService)
  }

  val originalCom = communityOffenderManager()
  val newCom = anotherCommunityOffenderManager()

  val licenceWithOriginalCom = createCrdLicence().copy(responsibleCom = originalCom)
  val licenceWithNewCom = createCrdLicence().copy(responsibleCom = newCom)

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
}
