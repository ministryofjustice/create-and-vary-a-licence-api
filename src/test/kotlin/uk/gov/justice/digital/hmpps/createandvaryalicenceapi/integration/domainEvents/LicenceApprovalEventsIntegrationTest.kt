package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.StatusUpdateRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.HMPPSDomainEvent
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.Message
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.util.LicenceStatus

class LicenceApprovalEventsIntegrationTest : IntegrationTestBase() {

  @ParameterizedTest
  @Sql("classpath:test_data/seed-licences-for-approval-events.sql")
  @CsvSource(
    "1, APPROVED, licence",
    "2, APPROVED, prrd-licence",
    "3, APPROVED, hdc-licence",
    "4, APPROVED, licence",
    "5, APPROVED, time-served-licence",
    "6, VARIATION_APPROVED, variation",
    "7, VARIATION_APPROVED, hdc-variation",
  )
  fun `Given a submitted licence when its status is approved then publish its approval event to SNS`(
    licenceId: Long,
    status: LicenceStatus,
    eventSubtype: String,
  ) {
    // Given
    val request = StatusUpdateRequest(status = status, username = "approver", fullName = "Test Approver")

    // When
    val response = webTestClient.put()
      .uri("/licence/id/$licenceId/status")
      .headers(setAuthorisation(roles = listOf(ROLE_CVL_ADMIN)))
      .contentType(MediaType.APPLICATION_JSON)
      .bodyValue(request)
      .exchange()

    // Then
    response.expectStatus().isOk
    assertApprovalEvent(licenceId, eventSubtype)
  }

  @ParameterizedTest
  @Sql("classpath:test_data/seed-licences-for-approval-events.sql")
  @CsvSource("6, 1, variation", "7, 3, hdc-variation")
  fun `Given a submitted variation when approved via the variation endpoint then publish its approval event to SNS`(
    licenceId: Long,
    parentId: Long,
    eventSubtype: String,
  ) {
    // Given
    assertThat(testRepository.findLicence(licenceId).statusCode).isEqualTo(LicenceStatus.VARIATION_SUBMITTED)

    // When
    val response = webTestClient.put()
      .uri("/licence/id/$licenceId/approve-variation")
      .headers(setAuthorisation(roles = listOf(ROLE_CVL_ADMIN)))
      .exchange()

    // Then
    response.expectStatus().isOk
    assertApprovalEvent(licenceId, eventSubtype)
  }

  private fun assertApprovalEvent(licenceId: Long, eventSubtype: String) {
    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 1 }

    val messages = domainEventsSqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(domainEventsQueueUrl).maxNumberOfMessages(10).build(),
    ).get().messages()
    assertThat(messages).hasSize(1)

    val envelope = mapper.readValue(messages.single().body(), Message::class.java)
    val event = mapper.readValue(envelope.message, HMPPSDomainEvent::class.java)
    val expectedEventType = "create-and-vary-a-licence.$eventSubtype.approved"
    val licence = testRepository.findLicence(licenceId)

    assertThat(envelope.messageAttributes.eventType.value).isEqualTo(expectedEventType)
    assertThat(envelope.messageAttributes.eventType.type).isEqualTo("String")
    assertThat(event.eventType).isEqualTo(expectedEventType)
    assertThat(event.additionalInformation).containsExactlyEntriesOf(mapOf("licenceId" to licenceId.toString()))
    assertThat(event.detailUrl).contains("/public/licences/id/$licenceId")
    assertThat(event.version).isEqualTo(1)
    assertThat(event.occurredAt).isNotNull()
    assertThat(event.description).isEqualTo("Licence approved for Licence ID $licenceId")
    assertThat(event.personReference.crn()).isEqualTo(licence.crn)
    assertThat(event.personReference.noms()).isEqualTo(licence.nomsId)
  }
}
