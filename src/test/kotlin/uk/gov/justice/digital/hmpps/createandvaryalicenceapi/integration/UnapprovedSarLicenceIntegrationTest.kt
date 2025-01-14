package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

class UnapprovedSarLicenceIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var notifyService: NotifyService

  @Test
  @Sql(
    "classpath:test_data/seed-unapproved-licences.sql",
  )
  fun `notifies probation of unapproved licences`() {
    webTestClient.post()
      .uri("/notify-probation-of-unapproved-licences")
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    val expectedUnapprovedLicences = listOf(
      UnapprovedLicence("H598679", "prisoner", "two", "Brian", "DDD", "testDDD@probation.gov.uk"),
      UnapprovedLicence("Z265290", "prisoner", "three", "Brian", "BBB", "testBBB@probation.gov.uk"),
    )
    verify(notifyService).sendUnapprovedLicenceEmail(expectedUnapprovedLicences)
  }
}
