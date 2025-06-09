package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config

import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import java.io.IOException
import java.net.ServerSocket

object PostgresContainer {

  private val log = LoggerFactory.getLogger(this::class.java)

  private const val DB_NAME = "create-and-vary-a-licence-db"
  private const val DB_DEFAULT_PORT = 5433
  const val DB_DEFAULT_URL = "jdbc:postgresql://localhost:$DB_DEFAULT_PORT/$DB_NAME?sslmode=prefer"
  const val DB_USERNAME = "cvl"
  const val DB_PASSWORD = "cvl"

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
}
