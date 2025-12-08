package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service

import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.groups.Tuple
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
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
import java.util.Optional

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
  fun reset() {
    val authentication = mock<Authentication>()
    val securityContext = mock<SecurityContext>()
    whenever(authentication.name).thenReturn("tcom1")
    whenever(securityContext.authentication).thenReturn(authentication)

    SecurityContextHolder.setContext(securityContext)

    reset(licenceRepository, auditEventRepository, auditService, notifyService, releaseDateService, staffRepository)
  }

  @Test
  fun `updates all in-flight licences associated with an offender with COM details`() {
    val originalCom = communityOffenderManager()
    val anotherCom = anotherCommunityOffenderManager()

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(licenceWithOriginalCom),
    )
    whenever(staffRepository.findByUsernameIgnoreCase(any())).thenReturn(anotherCom)

    service.updateResponsibleCom("exampleCrn", anotherCom)

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

    service.updateResponsibleCom("exampleCrn", newCom)

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

    service.updateResponsibleCom("exampleCrn", newCom)

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

    service.updateResponsibleCom("exampleCrn", newCom)

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
    val licence1 = licenceWithOriginalCom
    val licence2 = licence1.copy(id = 2, statusCode = LicenceStatus.ACTIVE)
    whenever(licenceRepository.findAllByNomsIdAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        licence1,
        licence2,
      ),
    )

    val expectedUpdatedLicences = listOf(
      licence1.copy(
        forename = newOffenderDetails.forename,
        middleNames = newOffenderDetails.middleNames,
        surname = newOffenderDetails.surname,
        dateOfBirth = newOffenderDetails.dateOfBirth,
      ),
      licence2.copy(
        forename = newOffenderDetails.forename,
        middleNames = newOffenderDetails.middleNames,
        surname = newOffenderDetails.surname,
        dateOfBirth = newOffenderDetails.dateOfBirth,
      ),
    )

    service.updateOffenderDetails(licenceWithOriginalCom.nomsId!!, newOffenderDetails)

    verify(licenceRepository).findAllByNomsIdAndStatusCodeIn(licenceWithOriginalCom.nomsId!!, IN_FLIGHT_LICENCES)
    verify(licenceRepository, times(1)).saveAllAndFlush(expectedUpdatedLicences)

    argumentCaptor<List<AuditEvent>>().apply {
      verify(auditEventRepository).saveAllAndFlush(capture())
      assertThat(firstValue).extracting("licenceId", "username", "fullName", "summary", "changes").isEqualTo(
        listOf(
          Tuple(
            1L,
            "SYSTEM",
            "SYSTEM",
            "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01",
            mapOf(
              "type" to "Updated offender details",
              "changes" to mapOf(
                "oldForename" to "John",
                "newForename" to newOffenderDetails.forename,
                "oldMiddleNames" to "",
                "newMiddleNames" to newOffenderDetails.middleNames,
                "oldSurname" to "Smith",
                "newSurname" to newOffenderDetails.surname,
                "oldDob" to LocalDate.parse("1985-12-28"),
                "newDob" to newOffenderDetails.dateOfBirth,
              ),
            ),
          ),
          Tuple(
            2L,
            "SYSTEM",
            "SYSTEM",
            "Offender details updated to forename: Peter, middleNames: Robin, surname: Smith, date of birth: 1970-02-01",
            mapOf(
              "type" to "Updated offender details",
              "changes" to mapOf(
                "oldForename" to "John",
                "newForename" to newOffenderDetails.forename,
                "oldMiddleNames" to "",
                "newMiddleNames" to newOffenderDetails.middleNames,
                "oldSurname" to "Smith",
                "newSurname" to newOffenderDetails.surname,
                "oldDob" to LocalDate.parse("1985-12-28"),
                "newDob" to newOffenderDetails.dateOfBirth,
              ),
            ),
          ),
        ),
      )
    }
  }

  @Test
  fun `does not send licence create email when all in-flight licences are created by prison`() {
    val prisonLicence = createTimeServedLicence().copy(
      statusCode = LicenceStatus.IN_PROGRESS,
    )
    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(prisonLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verifyNoInteractions(notifyService)
  }

  @Test
  fun `sends initial COM allocation email when time served licence exists and no previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence().copy(responsibleCom = null)

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(timeServedLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verify(notifyService, times(1)).sendInitialComAllocationEmail(
      emailAddress = newCom.email!!,
      comName = "${newCom.firstName} ${newCom.lastName}",
      offenderName = "${timeServedLicence.forename} ${timeServedLicence.surname}",
      crn = timeServedLicence.crn!!,
      licenceId = timeServedLicence.id.toString(),
    )
  }

  @Test
  fun `does not send initial COM allocation email when time served or variation licence exists and previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence().copy(responsibleCom = originalCom)
    val variationLicence = createVariationLicence().copy(responsibleCom = originalCom)

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(
      listOf(
        timeServedLicence,
        variationLicence,
      ),
    )
    whenever(staffRepository.findByUsernameIgnoreCase(originalCom.username)).thenReturn(originalCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verifyNoInteractions(notifyService)
  }

  @Test
  fun `does not send initial COM allocation email for CRD licences`() {
    val crdLicence = createCrdLicence()

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(crdLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verifyNoInteractions(notifyService)
  }

  @Test
  fun `sends initial COM allocation email when variation of time served licence exists and no previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence().copy(id = 1, responsibleCom = null)
    val variationLicence = createVariationLicence().copy(
      id = 2,
      responsibleCom = null,
      variationOfId = 1,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(variationLicence))
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(timeServedLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verify(notifyService, times(1)).sendInitialComAllocationEmail(
      emailAddress = newCom.email!!,
      comName = "${newCom.firstName} ${newCom.lastName}",
      offenderName = "${variationLicence.forename} ${variationLicence.surname}",
      crn = variationLicence.crn!!,
      licenceId = variationLicence.id.toString(),
    )
  }

  @Test
  fun `sends initial COM allocation email when variation of variation of time served licence exists and no previous COM was allocated`() {
    val timeServedLicence = createTimeServedLicence().copy(id = 1, responsibleCom = null)
    val firstVariationLicence = createVariationLicence().copy(
      id = 2,
      responsibleCom = null,
      variationOfId = 1,
    )
    val secondVariationLicence = createVariationLicence().copy(
      id = 3,
      responsibleCom = null,
      variationOfId = 2,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(secondVariationLicence))
    whenever(licenceRepository.findById(2L)).thenReturn(Optional.of(firstVariationLicence))
    whenever(licenceRepository.findById(1L)).thenReturn(Optional.of(timeServedLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    service.updateResponsibleCom("exampleCrn", newCom)

    verify(notifyService, times(1)).sendInitialComAllocationEmail(
      emailAddress = newCom.email!!,
      comName = "${newCom.firstName} ${newCom.lastName}",
      offenderName = "${secondVariationLicence.forename} ${secondVariationLicence.surname}",
      crn = secondVariationLicence.crn!!,
      licenceId = secondVariationLicence.id.toString(),
    )
  }

  @Test
  fun `throws EntityNotFoundException when parent licence not found in variation chain`() {
    val variationLicence = createVariationLicence().copy(
      id = 2,
      responsibleCom = null,
      variationOfId = 999,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(variationLicence))
    whenever(licenceRepository.findById(999L)).thenReturn(Optional.empty())
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    assertThrows<EntityNotFoundException> {
      service.updateResponsibleCom("exampleCrn", newCom)
    }
  }

  @Test
  fun `throws IllegalStateException when variation chain has no original licence`() {
    val firstVariationLicence = createVariationLicence().copy(
      id = 1,
      responsibleCom = null,
      variationOfId = null,
    )

    whenever(licenceRepository.findAllByCrnAndStatusCodeIn(any(), any())).thenReturn(listOf(firstVariationLicence))
    whenever(staffRepository.findByUsernameIgnoreCase(newCom.username)).thenReturn(newCom)

    assertThrows<IllegalStateException> {
      service.updateResponsibleCom("exampleCrn", newCom)
    }
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
