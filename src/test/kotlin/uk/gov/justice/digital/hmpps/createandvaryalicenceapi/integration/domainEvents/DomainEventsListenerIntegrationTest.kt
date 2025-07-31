package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.entity.CommunityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.WorkFlowMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.request.UpdateProbationTeamRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.AdditionalInformationPrisonerUpdated
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.COM_ALLOCATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DiffCategory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSPrisonerUpdatedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PersonReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerUpdatedHandler
import java.time.Duration

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["domain.event.listener.enabled=true"])
class DomainEventsListenerIntegrationTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var comAllocatedHandler: ComAllocatedHandler

  @MockitoSpyBean
  lateinit var prisonerUpdatedHandler: PrisonerUpdatedHandler

  @MockitoSpyBean
  lateinit var offenderService: OffenderService

  @MockitoSpyBean
  lateinit var domainEventListener: DomainEventListener

  @MockitoSpyBean
  lateinit var staffService: StaffService

  @Autowired
  lateinit var staffRepository: StaffRepository

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @Test
  fun `An COM allocated event from Delius is processed`() {
    // Given
    val crn = "X666322"
    val userName = "username"
    val emailAddress = "emailAddress@Delius"
    val staffIdentifier = 123L
    val firstName = "forename"
    val lastName = "surname"

    deliusMockServer.stubGetOffenderManager(
      crn,
      userName = userName,
      emailAddress,
      staffIdentifier,
      firstName = firstName,
      lastName = lastName,
    )
    deliusMockServer.stubAssignDeliusRole(userName = userName.uppercase())
    val event = buildComAllocatedEventJson(crn = crn, personUuid = null)
    val message = mapper.writeValueAsString(event)

    // When
    sendEvent(message, event.eventType)

    // Then
    verifyUpdateComDetails(
      staffIdentifier,
      userName,
      emailAddress,
      firstName = firstName,
      lastName = lastName,
    )
    verifyUpdateOffenderWithResponsibleCom(
      crn = crn,
      id = 9,
      staffIdentifier = staffIdentifier,
      username = userName,
      email = emailAddress,
      firstName = firstName,
      lastName = lastName,
    )
    verifyUpdateProbationTeam(
      crn = crn,
      teamCode = "team-code-1",
      teamDescription = "staff-description-1",
      areaCode = "probationArea-code-1",
      areaDescription = "probationArea-description-1",
      boroughCode = "borough-code-1",
      boroughDescription = "borough-description-1",
      districtCode = "district-code-1",
      districtDescription = "district-description-1",
    )

    assertComExistsInDb(staffIdentifier, userName, emailAddress, firstName, lastName)
  }

  @Test
  fun `An COM allocated event from APOP is processed`() {
    // Given
    val staffCode = "STAFF01CODE"
    val crn = "X666322"
    val userName = "username"
    val emailAddress = "testUser@test.justice.gov.uk"
    val personUuid = "1d9ab4b2-7b80-4784-8104-f9a77fd93a31"
    val teamCode = "teamCode"
    val staffIdentifier = 123456L
    val firstName = "forename"
    val lastName = "surname"

    val event = buildComAllocatedEventJson(crn = crn, personUuid = personUuid)
    workFlowMockServer.stubGetStaffDetails(personUuid, crn, staffCode, teamCode)
    deliusMockServer.stubGetStaffByCode(staffCode, userName, teamCode, firstName, lastName)
    deliusMockServer.stubAssignDeliusRole(userName = userName.uppercase())
    val message = mapper.writeValueAsString(event)

    // When
    sendEvent(message, event.eventType)

    // Then
    verify(comAllocatedHandler).handleEvent(message)
    verifyUpdateComDetails(staffIdentifier, userName, emailAddress, firstName, lastName)
    verifyUpdateOffenderWithResponsibleCom(
      crn = crn,
      id = 9,
      staffIdentifier = staffIdentifier,
      username = userName,
      email = emailAddress,
    )
    verifyUpdateProbationTeam(
      crn = crn,
      teamCode = teamCode,
      teamDescription = "Team description",
      areaCode = "PBC123",
      areaDescription = "Some Provider",
      boroughCode = "BC123",
      boroughDescription = "Borough description",
      districtCode = "DBC123",
      districtDescription = "District description",
    )

    assertComExistsInDb(staffIdentifier, userName, emailAddress, firstName, lastName)
  }

  @Test
  fun `A prisoner updated event is processed`() {
    // Given
    val nomsId = "A1234AA"
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    val sentEvent = HMPPSPrisonerUpdatedEvent(
      additionalInformation = AdditionalInformationPrisonerUpdated(
        nomsId,
        categoriesChanged = listOf(DiffCategory.PERSONAL_DETAILS),
      ),
      version = 1,
      occurredAt = "2025-04-07T11:56:00Z",
      description = "Prisoner updated",
    )

    val message = mapper.writeValueAsString(sentEvent)

    // When
    sendEvent(message, sentEvent.eventType)

    // Then
    verify(prisonerUpdatedHandler).handleEvent(message)
  }

  private fun sendEvent(message: String?, eventType: String?) {
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(message)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(eventType!!).build(),
          ),
        )
        .build(),
    )

    awaitAtMost30Secs untilAsserted {
      verify(domainEventListener).finishedEventProcessing(any())
    }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  private fun assertComExistsInDb(
    staffIdentifier: Long,
    userName: String,
    emailAddress: String,
    firstName: String = "Test",
    lastName: String = "Test",
  ) {
    val persisted = staffRepository.findByStaffIdentifier(staffIdentifier)
    assertThat(persisted).isNotNull
    assertThat(persisted!!.username).isEqualTo(userName.uppercase())
    assertThat(persisted.email).isEqualTo(emailAddress)
    assertThat(persisted.firstName).isEqualTo(firstName)
    assertThat(persisted.lastName).isEqualTo(lastName)
  }

  fun buildComAllocatedEventJson(
    crn: String,
    allocationId: String = "1d9ab4b2-7b80-4784-8104-f9a77fd93a31",
    occurredAt: String = "2025-04-07T11:56:00Z",
    description: String = "Person allocated event",
    eventType: String = COM_ALLOCATED_EVENT_TYPE,
    personUuid: String? = "1d9ab4b2-7b80-4784-8104-f9a77fd93a31",
    version: Int = 1,
  ): HMPPSDomainEvent = HMPPSDomainEvent(
    eventType = eventType,
    additionalInformation = mapOf("allocationId" to allocationId),
    detailUrl = personUuid?.let { "https://hmpps-workload.hmpps.service.justice.gov.uk/allocation/person/$it" },
    version = version,
    occurredAt = occurredAt,
    description = description,
    personReference = PersonReference(
      identifiers = listOf(Identifiers("CRN", crn)),
    ),
  )

  private fun verifyUpdateComDetails(
    staffIdentifier: Long = 123456,
    userName: String,
    email: String,
    firstName: String = "Test",
    lastName: String = "User",
  ) {
    verify(staffService).updateComDetails(
      UpdateComRequest(
        staffIdentifier = staffIdentifier,
        staffUsername = userName.trim().uppercase(),
        staffEmail = email,
        firstName = firstName,
        lastName = lastName,
      ),
    )
  }

  private fun verifyUpdateOffenderWithResponsibleCom(
    crn: String,
    id: Long,
    staffIdentifier: Long,
    username: String,
    email: String,
    firstName: String = "Test",
    lastName: String = "User",
  ) {
    verify(offenderService).updateOffenderWithResponsibleCom(
      crn,
      CommunityOffenderManager(
        id = id,
        staffIdentifier = staffIdentifier,
        username = username.uppercase(),
        email = email,
        firstName = firstName,
        lastName = lastName,
        lastUpdatedTimestamp = null,
      ),
    )
  }

  private fun verifyUpdateProbationTeam(
    crn: String,
    teamCode: String,
    teamDescription: String,
    areaCode: String,
    areaDescription: String,
    boroughCode: String,
    boroughDescription: String,
    districtCode: String,
    districtDescription: String,
  ) {
    verify(offenderService).updateProbationTeam(
      crn,
      UpdateProbationTeamRequest(
        probationAreaCode = areaCode,
        probationAreaDescription = areaDescription,
        probationPduCode = boroughCode,
        probationPduDescription = boroughDescription,
        probationLauCode = districtCode,
        probationLauDescription = districtDescription,
        probationTeamCode = teamCode,
        probationTeamDescription = teamDescription,
      ),
    )
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()
    val workFlowMockServer = WorkFlowMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      prisonerSearchMockServer.start()
      workFlowMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      prisonerSearchMockServer.stop()
      workFlowMockServer.stop()
    }
  }
}
