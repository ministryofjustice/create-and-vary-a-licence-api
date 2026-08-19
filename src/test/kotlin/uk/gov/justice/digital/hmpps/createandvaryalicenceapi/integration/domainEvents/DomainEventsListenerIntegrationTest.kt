package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.extensions.WorkFlowMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.StaffRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.OffenderService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.StaffService
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.TestData.communityOffenderManager
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.AdditionalInformationPrisonerMerged
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.AdditionalInformationPrisonerUpdated
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.COM_ALLOCATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DiffCategory
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DomainEventListener
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSPrisonerMergedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSPrisonerUpdatedEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Identifiers
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PRISONER_UPDATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PRISON_OFFENDER_MERGED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PersonReference
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerMergedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.PrisonerUpdatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.RECALL_INSERTED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.RECALL_UPDATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.RecallInsertedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.RecallUpdatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.events.UpdateProbationTeamEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.EligibleKind
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus.INACTIVE
import java.time.Duration
import java.time.LocalDate

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = ["domain.event.listener.disabled=false"])
class DomainEventsListenerIntegrationTest : IntegrationTestBase() {

  @MockitoSpyBean
  lateinit var comAllocatedHandler: ComAllocatedHandler

  @MockitoSpyBean
  lateinit var prisonerMergedHandler: PrisonerMergedHandler

  @MockitoSpyBean
  lateinit var prisonerUpdatedHandler: PrisonerUpdatedHandler

  @MockitoSpyBean
  lateinit var offenderService: OffenderService

  @MockitoSpyBean
  lateinit var domainEventListener: DomainEventListener

  @MockitoSpyBean
  lateinit var recallInsertedHandler: RecallInsertedHandler

  @MockitoSpyBean
  lateinit var recallUpdatedHandler: RecallUpdatedHandler

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
    val staffCode = "AB00001"
    val userName = "username"
    val emailAddress = "emailAddress@Delius"
    val staffIdentifier = 123L
    val firstName = "forename"
    val lastName = "surname"

    deliusMockServer.stubGetOffenderManager(
      crn = crn,
      staffCode = staffCode,
      userName = userName,
      emailAddress = emailAddress,
      staffIdentifier = staffIdentifier,
      firstName = firstName,
      lastName = lastName,
    )
    deliusMockServer.stubAssignDeliusRole(userName = userName.uppercase())
    val event = buildComAllocatedEventJson(crn = crn, personUuid = null)
    val message = mapper.writeValueAsString(event)

    // When
    sendEventAndVerifyProcessed(message, event.eventType)

    // Then
    verifyUpdateComDetails(
      staffIdentifier = staffIdentifier,
      email = emailAddress,
      staffCode = staffCode,
      userName = userName,
      firstName = firstName,
      lastName = lastName,
    )
    verifyUpdateOffenderWithResponsibleCom(
      crn = crn,
      id = 9,
      staffCode = staffCode,
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

    assertComExistsInDb(staffIdentifier, staffCode, userName, emailAddress, firstName, lastName)
  }

  @Test
  fun `An COM allocated event from APOP is processed`() {
    // Given
    val staffCode = "AB00001"
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
    sendEventAndVerifyProcessed(message, event.eventType)

    // Then
    verify(comAllocatedHandler).handleEvent(message)
    verifyUpdateComDetails(staffIdentifier, userName, staffCode, emailAddress, firstName, lastName)
    verifyUpdateOffenderWithResponsibleCom(
      crn = crn,
      id = 9,
      staffCode = staffCode,
      staffIdentifier = staffIdentifier,
      username = userName,
      email = emailAddress,
    )
    verifyUpdateProbationTeam(
      crn = crn,
      teamCode = teamCode,
      teamDescription = "Team description",
      areaCode = "PBC123",
      areaDescription = "Provider description",
      boroughCode = "BC123",
      boroughDescription = "Borough description",
      districtCode = "DBC123",
      districtDescription = "District description",
    )

    assertComExistsInDb(staffIdentifier, staffCode, userName, emailAddress, firstName, lastName)
  }

  private fun sendEventAndVerifyProcessed(message: String?, eventType: String?, eventsExpected: Int = 1) {
    sendEvent(message, eventType)

    awaitAtMost30Secs untilAsserted {
      // Two because we will also raise a licence inactivated event
      verify(domainEventListener, times(eventsExpected)).finishedEventProcessing(any())
    }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  private fun sendEvent(message: String?, eventType: String?) = domainEventsTopicSnsClient.publish(
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

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
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
    sendEventAndVerifyProcessed(message, sentEvent.eventType)

    // Then
    verify(prisonerUpdatedHandler).handleEvent(message)

    val licence = testRepository.findLicence(1)
    assertThat(licence.forename).isEqualTo("Test1")
    assertThat(licence.middleNames).isEqualTo("")
    assertThat(licence.surname).isEqualTo("Person1")
    assertThat(licence.dateOfBirth).isEqualTo("1985-01-01")

    val auditEvent = testRepository.findFirstAuditEvent(1)
    assertThat(auditEvent.summary).isEqualTo("Offender details updated to forename: Test1, middleNames: , surname: Person1, date of birth: 1985-01-01")
    assertThat(auditEvent.changes).isEqualTo(
      mapOf(
        "type" to "Updated offender details",
        "changes" to mapOf(
          "oldForename" to "Person",
          "newForename" to "Test1",
          "oldMiddleNames" to "",
          "newMiddleNames" to "",
          "oldSurname" to "One",
          "newSurname" to "Person1",
          "oldDob" to listOf(2020, 10, 25),
          "newDob" to listOf(1985, 1, 1),
        ),
      ),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-3.sql",
  )
  fun `A recall inserted event is processed`() {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetSentenceAndRecallTypesWithStandardRecall()

    val event = HMPPSDomainEvent(
      eventType = RECALL_INSERTED_EVENT_TYPE,
      additionalInformation = mapOf(
        "source" to "NOMIS",
        "recallId" to "dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
        "sentenceIds" to "[c2a7159c-383a-4a98-9f00-7c410b6e1900]",
      ),
      detailUrl = "https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/recall/dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
      version = 1,
      occurredAt = "2026-03-27T09:27:38.6679417Z",
      description = "Recall inserted",
      personReference = PersonReference(
        identifiers = listOf(Identifiers("NOMS", "A1234AA")),
      ),
    )

    val message = mapper.writeValueAsString(event)

    // wait for two events as a licence deactivation event will be generated by the recall inserted handler
    sendEventAndVerifyProcessed(message, event.eventType, 2)

    verify(recallInsertedHandler).handleEvent(message)

    val licence = testRepository.findLicence(3)
    assertThat(licence.forename).isEqualTo("Person")
    assertThat(licence.surname).isEqualTo("One")
    assertThat(licence.statusCode).isEqualTo(INACTIVE)

    val auditEvent = testRepository.findFirstAuditEvent(3)
    assertThat(auditEvent.summary).isEqualTo("Licence inactivated due to the offender returning to custody on a standard recall for Person One")
    assertThat(auditEvent.changes).isNull()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-prrd-licence-id-1.sql",
  )
  fun `A recall updated event is processed and converts a fixed term recall to a standard recall licence`() {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds()
    prisonApiMockServer.stubGetSentenceAndRecallTypesWithStandardRecall()

    val event = HMPPSDomainEvent(
      eventType = RECALL_UPDATED_EVENT_TYPE,
      additionalInformation = mapOf(
        "source" to "NOMIS",
        "recallId" to "dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
        "previousRecallId" to "4c1f5640-c278-4682-ba2e-d16ee438bd75",
        "sentenceIds" to "[c2a7159c-383a-4a98-9f00-7c410b6e1900]",
      ),
      detailUrl = "https://remand-and-sentencing-api-dev.hmpps.service.justice.gov.uk/recall/dfd1e5c2-318c-4f56-b4c8-2d236696e52c",
      version = 1,
      occurredAt = "2026-03-27T09:27:38.6679417Z",
      description = "Recall updated",
      personReference = PersonReference(
        identifiers = listOf(Identifiers("NOMS", "A1234AA")),
      ),
    )

    val message = mapper.writeValueAsString(event)

    sendEventAndVerifyProcessed(message, event.eventType)

    verify(recallUpdatedHandler).handleEvent(message)

    val licence = testRepository.findLicence()
    assertThat(licence.eligibleKind).isEqualTo(EligibleKind.STANDARD)

    val auditEvent = testRepository.findFirstAuditEvent()
    assertThat(auditEvent.summary).isEqualTo("Licence kind updated on licence for Person One")
    assertThat(auditEvent.changes).isEqualTo(
      mapOf(
        "type" to "Licence kind updated on licence",
        "changes" to mapOf(
          "oldKind" to "PRRD",
          "newKind" to "PRRD",
          "oldEligibleKind" to "FIXED_TERM",
          "newEligibleKind" to "STANDARD",
        ),
      ),
    )
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql",
  )
  fun `A supporting prison changed event is processed`() {
    prisonerSearchMockServer.stubSearchPrisonersByNomisIds(aRestrictedPatientRecord)
    prisonApiMockServer.stubGetPrison()

    val currentLicence = testRepository.findLicence()

    val event = HMPPSPrisonerUpdatedEvent(
      eventType = PRISONER_UPDATED_EVENT_TYPE,
      additionalInformation = AdditionalInformationPrisonerUpdated(
        nomsNumber = "A1234AA",
        categoriesChanged = listOf(DiffCategory.RESTRICTED_PATIENT),
      ),
      version = 1,
      occurredAt = "2026-05-22T00:00:00Z",
      description = "A prisoner record has been updated",
    )

    val message = mapper.writeValueAsString(event)

    sendEventAndVerifyProcessed(message, event.eventType)

    verify(prisonerUpdatedHandler).handleEvent(message)

    val updatedLicence = testRepository.findLicence()
    assertThat(currentLicence.prisonCode).isEqualTo("MDI")
    assertThat(updatedLicence.prisonCode).isEqualTo("ABC")

    val auditEvent = testRepository.findFirstAuditEvent()
    assertThat(auditEvent.summary).isEqualTo("Supporting prison information changed for Person One")
    assertThat(auditEvent.changes)
      .containsEntry("field", "prisonCode")
      .containsEntry("previousValue", "MDI")
      .containsEntry("newValue", "ABC")
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-to-merge.sql",
  )
  fun `A prisoner merged event is processed`() {
    val oldNomisId = "G5678XT"
    val newNomisId = "A1234AA"

    prisonApiMockServer.stubGetPrisonerDetail(nomsId = newNomisId)
    deliusMockServer.stubGetProbationCase()

    val event = HMPPSPrisonerMergedEvent(
      eventType = PRISON_OFFENDER_MERGED_EVENT_TYPE,
      additionalInformation = AdditionalInformationPrisonerMerged(
        bookingId = "123",
        nomsNumber = newNomisId,
        removedNomsNumber = oldNomisId,
        reason = "MERGED",
      ),
      version = 1,
      occurredAt = "2026-05-22T00:00:00Z",
      description = "A prisoner record has been updated",
    )

    val message = mapper.writeValueAsString(event)

    // Excpect two events as a licence deactivation event will be generated when deactivating the old licence
    sendEventAndVerifyProcessed(message, event.eventType, 2)

    verify(prisonerMergedHandler).handleEvent(message)

    val oldBookingLicence = testRepository.findLicence(1L)
    assertThat(oldBookingLicence.statusCode).isEqualTo(INACTIVE)

    val auditEvents = testRepository.findAllAuditEvents()
    assertThat(auditEvents).hasSize(2)
    assertThat(auditEvents.first().summary).isEqualTo("Deactivating licence on old booking after prisoner merge for Person One")

    val mergedAuditEvent = auditEvents[1]
    assertThat(mergedAuditEvent.summary).isEqualTo("Prisoner merged event for A Prisoner")
    assertThat(mergedAuditEvent.changes).isEqualTo(
      mapOf(
        "type" to "Updating licence details due to prisoner merge event",
        "changes" to mapOf(
          "oldNomisId" to oldNomisId,
          "newNomisId" to newNomisId,
          "oldForename" to "Person",
          "newForename" to "A",
          "oldMiddleName" to null,
          "newMiddleName" to null,
          "oldSurname" to "One",
          "newSurname" to "Prisoner",
          "oldPrisonCode" to "MDI",
          "newPrisonCode" to "ABC",
          "oldDateOfBirth" to "2020-10-25",
          "newDateOfBirth" to "1985-12-28",
          "oldCRO" to "CRO1",
          "newCRO" to "SF39/6W",
          "oldPNC" to "2015/1234",
          "newPNC" to "7428/85493",
        ),
      ),
    )

    val newBookingLicence = testRepository.findLicence(2L)
    assertThat(newBookingLicence.nomsId).isEqualTo(newNomisId)
    assertThat(newBookingLicence.forename).isEqualTo("A")
    assertThat(newBookingLicence.surname).isEqualTo("Prisoner")
  }

  private fun assertComExistsInDb(
    staffIdentifier: Long,
    staffCode: String,
    userName: String,
    emailAddress: String,
    firstName: String = "Test",
    lastName: String = "Test",
  ) {
    val persisted = staffRepository.findByStaffIdentifier(staffIdentifier)
    assertThat(persisted).isNotNull
    assertThat(persisted!!.username).isEqualTo(userName.uppercase())
    assertThat(persisted.staffCode).isEqualTo(staffCode)
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
    staffCode: String,
    email: String,
    firstName: String = "Test",
    lastName: String = "User",
  ) {
    verify(staffService).updateComDetails(
      UpdateComRequest(
        staffIdentifier = staffIdentifier,
        staffCode = staffCode,
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
    staffCode: String,
    staffIdentifier: Long,
    username: String,
    email: String,
    firstName: String = "Test",
    lastName: String = "User",
  ) {
    verify(offenderService).updateResponsibleCom(
      crn,
      communityOffenderManager().copy(
        id = id,
        staffCode = staffCode,
        staffIdentifier = staffIdentifier,
        username = username,
        email = email,
        firstName = firstName,
        lastName = lastName,
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
      UpdateProbationTeamEvent(
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
    @RegisterExtension
    val deliusMockServer = DeliusMockServer()

    @RegisterExtension
    val prisonApiMockServer = PrisonApiMockServer()

    @RegisterExtension
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @RegisterExtension
    val workFlowMockServer = WorkFlowMockServer()

    val aRestrictedPatientRecord = """[{
      "prisonerNumber": "A1234AA",
      "bookingId": "123",
      "status": "INACTIVE OUT",
      "mostSeriousOffence": "Robbery",
      "licenceExpiryDate": "${LocalDate.now().plusYears(1)}",
      "topupSupervisionExpiryDate": "${LocalDate.now().plusYears(1)}",
      "homeDetentionCurfewEligibilityDate": null,
      "releaseDate": null,
      "confirmedReleaseDate": null,
      "conditionalReleaseDate": null,
      "paroleEligibilityDate": null,
      "actualParoleDate" : null,
      "postRecallReleaseDate": null,
      "legalStatus": "SENTENCED",
      "indeterminateSentence": false,
      "recall": false,
      "restrictedPatient": true,
      "prisonId": "OUT",
      "bookNumber": "12345A",
      "firstName": "Test1",
      "lastName": "Person1",
      "dateOfBirth": "1985-01-01",
      "supportingPrisonId": "ABC"
    }]"""
  }
}
