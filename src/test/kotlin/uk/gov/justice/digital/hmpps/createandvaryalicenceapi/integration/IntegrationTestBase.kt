package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OAuthExtension
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException

/*
** The abstract parent class for integration tests.
**
**  It supplies : -
**     - The SpringBootTest annotation.
**     - The active profile "test"
**     - An extension class providing a Wiremock hmpps-auth server.
**     - A JwtAuthHelper function.
**     - A WebTestClient.
**     - An ObjectMapper called mapper.
**     - A logger.
*/
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@ExtendWith(OAuthExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@Sql(
  "classpath:test_data/clear-all-data.sql",
  "classpath:test_data/seed-community-offender-manager.sql",
)
abstract class IntegrationTestBase {

  @BeforeEach
  fun `Clear queues`() {
    domainEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build())
  }

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents")
      ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val domainEventsQueue by lazy { hmppsQueueService.findByQueueId("domaineventsqueue") ?: throw MissingQueueException("HmppsQueue domaineventsqueue not found") }

  protected val domainEventsSqsClient by lazy { domainEventsQueue.sqsClient }
  protected val domainEventsQueueUrl by lazy { domainEventsQueue.queueUrl }

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var mapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @SpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  fun HmppsSqsProperties.domaineventsTopicConfig() =
    topics["domainevents"]
      ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  internal fun setAuthorisation(
    user: String = "test-client",
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)

  data class EventType(val Value: String, val Type: String)
  data class MessageAttributes(val eventType: EventType)
  data class Message(
    val Message: String,
    val MessageId: String,
    val MessageAttributes: MessageAttributes,
  )

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun testcontainers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }
}
