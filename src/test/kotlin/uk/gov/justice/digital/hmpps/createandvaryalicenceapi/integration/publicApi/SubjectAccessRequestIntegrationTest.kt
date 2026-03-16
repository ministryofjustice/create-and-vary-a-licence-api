package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.publicApi

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.DocumentApiMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServerExtension
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import javax.sql.DataSource

@ExtendWith(GovUkMockServerExtension::class)
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestIntegrationTest :
  IntegrationTestBase(),
  SarApiDataTest,
  SarFlywaySchemaTest,
  SarReportTest,
  SarJpaEntitiesTest {
  @Autowired
  lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getPrn(): String = SAR_PRN

  override fun setupTestData() {
  }

  @Test
  @Sql("classpath:test_data/seed-sar-content-licence-id.sql")
  override fun `SAR API should return expected data`() {
    documentApiMockServerExtension.documentMockServer.stubDownloadImage(
      "92939445-4159-4214-aa75-d07568a3e136",
    )
    super.`SAR API should return expected data`()
  }

  @Test
  @Sql("classpath:test_data/seed-sar-content-licence-id.sql")
  override fun `SAR report should render as expected`() {
    documentApiMockServerExtension.documentMockServer.stubDownloadImage(
      "92939445-4159-4214-aa75-d07568a3e136",
    )
    super.`SAR report should render as expected`()
  }

  companion object {
    @JvmStatic
    @RegisterExtension
    val documentApiMockServerExtension = DocumentApiMockServer.Extension()
    private const val SAR_PRN = "A1234AA"
  }
}
