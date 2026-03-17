package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.reset
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.AFTER_TEST_METHOD
import org.springframework.test.context.jdbc.Sql.ExecutionPhase.BEFORE_TEST_METHOD
import org.springframework.test.context.jdbc.SqlConfig
import org.springframework.test.context.jdbc.SqlGroup
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.LocalStackContainer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PostgresContainer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PostgresContainer.setPostgresContainerProperties
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OAuthExtension
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsSqsProperties
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.MissingTopicException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

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

@ExtendWith(OAuthExtension::class)
@ActiveProfiles("test")
@SpringBootTest(
  webEnvironment = RANDOM_PORT,
)
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
@SqlGroup(
  Sql("classpath:test_data/seed-community-offender-manager.sql", executionPhase = BEFORE_TEST_METHOD),
  Sql("classpath:test_data/clear-all-data.sql", executionPhase = AFTER_TEST_METHOD),
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@AutoConfigureWebTestClient(timeout = "25000") // 25 seconds
abstract class IntegrationTestBase {

  @MockitoSpyBean
  lateinit var telemetryClient: TelemetryClient

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  protected lateinit var testRepository: TestRepository

  protected val domainEventsTopic by lazy {
    hmppsQueueService.findByTopicId("domainevents") ?: throw MissingQueueException("HmppsTopic domainevents not found")
  }
  protected val domainEventsTopicSnsClient by lazy { domainEventsTopic.snsClient }
  protected val domainEventsTopicArn by lazy { domainEventsTopic.arn }

  protected val domainEventsQueue by lazy {
    hmppsQueueService.findByQueueId("domaineventsqueue")
      ?: throw MissingQueueException("HmppsQueue domaineventsqueue not found")
  }

  protected val domainEventsSqsClient by lazy { domainEventsQueue.sqsClient }
  protected val domainEventsQueueUrl by lazy { domainEventsQueue.queueUrl }

  protected val prisonEventsTopic by lazy {
    hmppsQueueService.findByTopicId("prisonevents")
      ?: throw MissingQueueException("HmppsTopic prisonevents not found")
  }

  protected val prisonEventsTopicSnsClient by lazy { prisonEventsTopic.snsClient }
  protected val prisonEventsTopicArn by lazy { prisonEventsTopic.arn }

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var mapper: ObjectMapper

  @MockitoSpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  fun HmppsSqsProperties.domaineventsTopicConfig() = topics["domainevents"]
    ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  internal fun setAuthorisation(
    user: String = "test-client",
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)

  init {
    // Resolves an issue where Wiremock keeps previous sockets open from other tests causing connection resets
    System.setProperty("http.keepAlive", "false")
  }

  @BeforeEach
  fun `Clear queues`() {
    domainEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build())
    await untilCallTo {
      domainEventsQueue.sqsClient.countMessagesOnQueue(domainEventsQueue.queueUrl).get()
    } matches { it == 0 }
    reset(telemetryClient)
  }

  @AfterEach
  fun tearDown() {
    testRepository.clearAll()
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? = domainEventsQueue.sqsClient.countMessagesOnQueue(domainEventsQueueUrl).get()

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val localStackContainer = LocalStackContainer.instance
    private val postgresContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun containers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }
      postgresContainer?.also { setPostgresContainerProperties(it, registry) }
    }
  }
}
