package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.domainEvents

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilAsserted
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DeliusMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.PrisonerSearchMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.AdditionalInformationPrisonerUpdated
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.COM_ALLOCATED_EVENT_TYPE
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.DiffCategory
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

  private val awaitAtMost30Secs
    get() = await.atMost(Duration.ofSeconds(30))

  @Test
  fun `An COM allocated event is processed`() {
    val crn = "X666322"
    deliusMockServer.stubGetOffenderManager(crn)

    val sentEvent = HMPPSDomainEvent(
      COM_ALLOCATED_EVENT_TYPE,
      additionalInformation = mapOf("allocationId" to "1d9ab4b2-7b80-4784-8104-f9a77fd93a31"),
      "\"https://hmpps-workload.hmpps.service.justice.gov.uk/allocation/person/1d9ab4b2-7b80-4784-8104-f9a77fd93a31",
      1,
      "2025-04-07T11:56:00Z",
      "Person allocated event",
      PersonReference(
        listOf(
          Identifiers("CRN", crn),
        ),
      ),
    )

    val message = mapper.writeValueAsString(sentEvent)
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(message)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(sentEvent.eventType).build(),
          ),
        )
        .build(),
    ).get()

    awaitAtMost30Secs untilAsserted {
      verify(comAllocatedHandler).handleEvent(message)
    }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  @Test
  fun `A prisoner updated event is processed`() {
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
    domainEventsTopicSnsClient.publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopicArn)
        .message(message)
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(sentEvent.eventType).build(),
          ),
        )
        .build(),
    ).get()

    awaitAtMost30Secs untilAsserted {
      verify(prisonerUpdatedHandler).handleEvent(message)
    }
    assertThat(getNumberOfMessagesCurrentlyOnQueue()).isEqualTo(0)
  }

  private companion object {
    val deliusMockServer = DeliusMockServer()
    val prisonerSearchMockServer = PrisonerSearchMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      deliusMockServer.start()
      prisonerSearchMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      deliusMockServer.stop()
      prisonerSearchMockServer.stop()
    }
  }
}
