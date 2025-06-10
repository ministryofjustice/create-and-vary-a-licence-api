package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.slf4j.LoggerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {

  private val log = LoggerFactory.getLogger(this::class.java)

  private val DB_NAME = "create-and-vary-a-licence-db"
  private val DB_DEFAULT_PORT = 5433
  private val DB_DEFAULT_URL = "jdbc:postgresql://localhost:$DB_DEFAULT_PORT/$DB_NAME?sslmode=prefer"
  private val DB_USERNAME = "cvl"
  private val DB_PASSWORD = "cvl"

  val instance: PostgreSQLContainer<Nothing>? by lazy { startPostgresqlContainer() }

  private fun startPostgresqlContainer(): PostgreSQLContainer<Nothing>? {
    if (isPostgresRunning()) {
      return null
    }

    val logConsumer = Slf4jLogConsumer(log).withPrefix("postgresql")

    return PostgreSQLContainer<Nothing>("postgres:17.5").apply {
      withEnv("HOSTNAME_EXTERNAL", "localhost")
      withDatabaseName(DB_NAME)
      withUsername(DB_USERNAME)
      withPassword(DB_PASSWORD)
      setWaitStrategy(Wait.forListeningPort())
      withReuse(true)
      withTmpFs(mapOf("/var/lib/postgresql/data" to "rw"))
      start()
      followOutput(logConsumer)
    }
  }

  private fun isPostgresRunning(): Boolean = try {
    val serverSocket = ServerSocket(DB_DEFAULT_PORT)
    serverSocket.localPort == 0
  } catch (e: IOException) {
    true
  }

  fun setPostgresContainerProperties(instance:  PostgreSQLContainer<Nothing>?, registry: DynamicPropertyRegistry) {
    val url = instance?.let { instance.jdbcUrl } ?: DB_DEFAULT_URL
    IntegrationTestBase.log.info("Using TestContainers?: ${instance != null}, DB url: $url")
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
