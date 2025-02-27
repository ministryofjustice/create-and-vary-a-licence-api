package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.wiremock.GovUkMockServer
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService
import java.time.Duration

class HardStopLicenceReviewOverdueIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @MockitoBean
  lateinit var notifyService: NotifyService

  @BeforeEach
  fun setupClient() {
    webTestClient = webTestClient.mutate().responseTimeout(Duration.ofSeconds(60)).build()
    govUkApiMockServer.stubGetBankHolidaysForEnglandAndWales()
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licences-for-hard-stop-review.sql",
  )
  fun `Run hard stop licence review overdue job`() {
    webTestClient.post()
      .uri("/jobs/warn-hard-stop-review-overdue")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isOk

    verify(notifyService, times(1)).sendHardStopLicenceReviewOverdueEmail(
      emailAddress = "testClient@probation.gov.uk",
      comName = "Test Client",
      firstName = "Test Forename 1",
      lastName = "Test Surname 1",
      crn = "A123456",
      licenceId = "1",
    )
  }

  private companion object {
    val govUkApiMockServer = GovUkMockServer()

    @JvmStatic
    @BeforeAll
    fun startMocks() {
      govUkApiMockServer.start()
    }

    @JvmStatic
    @AfterAll
    fun stopMocks() {
      govUkApiMockServer.stop()
    }
  }
}
