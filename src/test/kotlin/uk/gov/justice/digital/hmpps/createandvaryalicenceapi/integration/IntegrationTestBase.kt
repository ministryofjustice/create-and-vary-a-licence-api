package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.gson.GsonBuilder
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.helpers.JwtAuthHelper
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.OAuthExtension

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
  "classpath:test_data/seed-community-offender-manager.sql"
)
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var mapper: ObjectMapper

  internal val gson = GsonBuilder().setPrettyPrinting().create()

  internal fun setAuthorisation(
    user: String = "test-client",
    roles: List<String> = listOf()
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, roles)

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }
}
