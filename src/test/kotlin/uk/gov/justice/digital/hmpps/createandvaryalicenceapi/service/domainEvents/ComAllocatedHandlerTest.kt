package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.ResponseEntity
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateProbationTeamEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.CommunityManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.DeliusApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.User
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.WorkLoadApiClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.mapper.OffenderManagerMapper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.OffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.probation.model.response.WorkLoadAllocationResponse
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ComAllocatedHandlerTest {

  private val deliusApiClient = mock<DeliusApiClient>()
  private val workLoadApiClient = mock<WorkLoadApiClient>()
  private val offenderService = mock<OffenderService>()
  private val staffService = mock<StaffService>()
  private val offenderManagerMapper = mock<OffenderManagerMapper>()
  private val objectMapper = jacksonObjectMapper()
  private val handler = ComAllocatedHandler(
    deliusApiClient,
    workLoadApiClient,
    offenderService,
    objectMapper,
    staffService,
    offenderManagerMapper,
  )

  private val updateComRequestCaptor = argumentCaptor<UpdateComRequest>()
  private val updateProbationTeamEventCaptor = argumentCaptor<UpdateProbationTeamEvent>()
  private val crn = "TEST123456"

  @BeforeEach
  fun setup() {
    reset(deliusApiClient, offenderService, staffService, offenderManagerMapper)
  }

  @Test
  fun `handleEvent processes valid Delius event should allocate com`() {
    // Given
    val eventJson = buildEventJson(crn, detailUrl = null)
    val deliusResponse = mock<CommunityManager>()
    val offenderManager = buildOffenderManager()
    val com = mock<CommunityOffenderManager>()

    whenever(deliusApiClient.getOffenderManager(crn)).thenReturn(deliusResponse)
    whenever(offenderManagerMapper.mapFrom(deliusResponse)).thenReturn(offenderManager)
    whenever(deliusApiClient.assignDeliusRole(offenderManager.username!!)).thenReturn(ResponseEntity.ok().build())
    whenever(staffService.updateComDetails(any())).thenReturn(com)

    doNothing().whenever(offenderService).updateResponsibleCom(crn, com)
    doNothing().whenever(offenderService).updateProbationTeam(anyString(), any())

    // When
    handler.handleEvent(eventJson)

    // Then
    assertProcess(crn, offenderManager, com)
  }

  @Test
  fun `handleEvent processes valid Apop event should allocate com`() {
    // Given
    val personUuid = UUID.randomUUID().toString()
    val detailUrl = "/allocation/person/$personUuid"
    val eventJson = buildEventJson(crn, detailUrl)
    val staffCodes = mock<WorkLoadAllocationResponse>()
    val staff = mock<User>()
    val offenderManager = buildOffenderManager()
    val com = mock<CommunityOffenderManager>()

    whenever(workLoadApiClient.getStaffDetails(personUuid)).thenReturn(staffCodes)
    whenever(deliusApiClient.getStaffByCode(staffCodes.staffCode)).thenReturn(staff)
    whenever(offenderManagerMapper.mapFrom(staff, staffCodes)).thenReturn(offenderManager)
    whenever(deliusApiClient.assignDeliusRole(offenderManager.username!!)).thenReturn(ResponseEntity.ok().build())
    whenever(staffService.updateComDetails(any())).thenReturn(com)
    whenever(staffService.findCommunityOffenderManager(any(), any())).thenReturn(
      emptyList(),
    )
    doNothing().whenever(offenderService).updateResponsibleCom(crn, com)
    doNothing().whenever(offenderService).updateProbationTeam(anyString(), any())

    // When
    handler.handleEvent(eventJson)

    // Then
    assertProcess(crn, offenderManager, com)
  }

  @Test
  fun `isValidOffenderManager returns false when offenderManager is null`() {
    // When
    val result = handler.isValidOffenderManager(null, crn)

    // Then
    assertThat(result).isFalse()
  }

  @Test
  fun `isValidOffenderManager returns false when username is blank`() {
    // Given
    val manager = buildOffenderManager(username = "   ")

    // When
    val result = handler.isValidOffenderManager(manager, crn)

    // Then
    assertThat(result).isFalse()
  }

  @Test
  fun `isValidOffenderManager returns true when valid offenderManager`() {
    // Given
    val manager = buildOffenderManager()

    // When
    val result = handler.isValidOffenderManager(manager, crn)

    // Then
    assertThat(result).isTrue()
  }

  @Test
  fun `handleEvent logs and exits when JSON invalid`() {
    // Given
    val invalidJson = "not a json"

    // When
    handler.handleEvent(invalidJson)

    // Then
    // No exception thrown, logs internally
  }

  @Test
  fun `getPersonUuid extracts correct substring`() {
    // Given
    val uuid = UUID.randomUUID().toString()
    val detailUrl = "/allocation/person/$uuid"

    // When
    val result = handler.getPersonUuid(detailUrl)

    // Then
    assertThat(result).isEqualTo(uuid)
  }

  // Helpers
  private fun assertProcess(
    crn: String,
    offenderManager: OffenderManager,
    com: CommunityOffenderManager,
  ) {
    verify(deliusApiClient).assignDeliusRole(offenderManager.username!!)
    verify(staffService).updateComDetails(updateComRequestCaptor.capture())
    verify(offenderService).updateResponsibleCom(crn, com)
    verify(offenderService).updateProbationTeam(eq(crn), updateProbationTeamEventCaptor.capture())

    val capturedRequest = updateComRequestCaptor.firstValue
    assertThat(capturedRequest.staffIdentifier).isEqualTo(offenderManager.staffIdentifier)
    assertThat(capturedRequest.staffUsername).isEqualTo(offenderManager.username)
    assertThat(capturedRequest.staffEmail).isEqualTo(offenderManager.email)
    assertThat(capturedRequest.firstName).isEqualTo(offenderManager.forename)
    assertThat(capturedRequest.lastName).isEqualTo(offenderManager.surname)

    val capturedTeamRequest = updateProbationTeamEventCaptor.firstValue
    assertThat(capturedTeamRequest.probationAreaCode).isEqualTo(offenderManager.providerCode)
    assertThat(capturedTeamRequest.probationAreaDescription).isEqualTo(offenderManager.providerDescription)
    assertThat(capturedTeamRequest.probationPduCode).isEqualTo(offenderManager.boroughCode)
    assertThat(capturedTeamRequest.probationPduDescription).isEqualTo(offenderManager.boroughDescription)
    assertThat(capturedTeamRequest.probationLauCode).isEqualTo(offenderManager.districtCode)
    assertThat(capturedTeamRequest.probationLauDescription).isEqualTo(offenderManager.districtDescription)
    assertThat(capturedTeamRequest.probationTeamCode).isEqualTo(offenderManager.teamCode)
    assertThat(capturedTeamRequest.probationTeamDescription).isEqualTo(offenderManager.teamDescription)
  }

  private fun buildOffenderManager(
    crn: String = this.crn,
    username: String? = "test.user",
    code: String = "CODE123",
  ) = OffenderManager(
    crn = crn,
    username = username,
    code = code,
    staffIdentifier = 9999L,
    email = "test.user@example.com",
    forename = "Test",
    surname = "User",
    providerCode = "PA01",
    providerDescription = "Test Area",
    boroughCode = "BC01",
    boroughDescription = "Test Borough",
    districtCode = "DC01",
    districtDescription = "Test District",
    teamCode = "TM01",
    teamDescription = "Test Team",
  )

  private fun buildEventJson(crn: String, detailUrl: String? = "/allocation/person/${UUID.randomUUID()}"): String = objectMapper.writeValueAsString(
    HMPPSDomainEvent(
      eventType = COM_ALLOCATED_EVENT_TYPE,
      version = 0,
      occurredAt = "2023-12-05T00:00:00Z",
      personReference = PersonReference(listOf(Identifiers("CRN", crn))),
      detailUrl = detailUrl,
    ),
  )
}
