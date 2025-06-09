package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.microsoft.applicationinsights.TelemetryClient
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
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PostgresContainer.DB_DEFAULT_URL
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PostgresContainer.DB_PASSWORD
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.PostgresContainer.DB_USERNAME
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OAuthExtension
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.domainEvents.ComAllocatedHandler
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
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = ["spring.jpa.properties.hibernate.enable_lazy_load_no_trans=false"])
@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED)
@SqlGroup(
  Sql("classpath:test_data/seed-community-offender-manager.sql", executionPhase = BEFORE_TEST_METHOD),
  Sql("classpath:test_data/clear-all-data.sql", executionPhase = AFTER_TEST_METHOD),
)
@AutoConfigureWebTestClient(timeout = "40000") // 30 seconds
@Transactional(propagation = Propagation.NOT_SUPPORTED)
abstract class IntegrationTestBase {

  @MockitoSpyBean
  lateinit var telemetryClient: TelemetryClient

  @MockitoSpyBean
  lateinit var comAllocatedHandler: ComAllocatedHandler

  @BeforeEach
  fun `Clear queues`() {
    domainEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventsQueue.queueUrl).build())
    reset(telemetryClient)
  }

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

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var mapper: ObjectMapper

  @Autowired
  protected lateinit var hmppsQueueService: HmppsQueueService

  @MockitoSpyBean
  protected lateinit var hmppsSqsPropertiesSpy: HmppsSqsProperties

  fun HmppsSqsProperties.domaineventsTopicConfig() = topics["domainevents"]
    ?: throw MissingTopicException("domainevents has not been loaded from configuration properties")

  internal fun setAuthorisation(
    user: String = "test-client",
    roles: List<String> = listOf(),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? = domainEventsQueue.sqsClient.countMessagesOnQueue(domainEventsQueueUrl).get()

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
    private val localStackContainer = LocalStackContainer.instance
    private val postgresContainer = PostgresContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun containers(registry: DynamicPropertyRegistry) {
      localStackContainer?.also { setLocalStackProperties(it, registry) }

      val url = postgresContainer?.let { postgresContainer.jdbcUrl } ?: DB_DEFAULT_URL
      log.info("Using TestContainers?: ${postgresContainer != null}, DB url: $url")
      registry.add("spring.datasource.url") { url }
      registry.add("spring.datasource.username") { DB_USERNAME }
      registry.add("spring.datasource.password") { DB_PASSWORD }
      registry.add("spring.flyway.url") { url }
      registry.add("spring.flyway.user") { DB_USERNAME }
      registry.add("spring.flyway.password") { DB_PASSWORD }
      registry.add("spring.datasource.placeholders.database_update_password") { DB_PASSWORD }
      registry.add("spring.datasource.placeholders.database_read_only_password") { DB_USERNAME }
    }
  }
}
