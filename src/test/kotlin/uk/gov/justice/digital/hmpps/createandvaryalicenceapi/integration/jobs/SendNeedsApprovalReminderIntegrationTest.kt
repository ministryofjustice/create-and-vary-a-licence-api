package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.junit.jupiter.api.Test
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UnapprovedLicence
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

class SendNeedsApprovalReminderIntegrationTest : IntegrationTestBase() {

  @MockitoBean
  lateinit var notifyService: NotifyService

  @Test
  @Sql(
    "classpath:test_data/seed-unapproved-licences.sql",
  )
  fun `notifies probation of unapproved licences`() {
    // Given
    val uri = "/jobs/notify-probation-of-unapproved-licences"

    // When
    val result = webTestClient.post()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()

    // Then
    result.expectStatus().isOk

    val expectedUnapprovedLicences = listOf(
      UnapprovedLicence("H598679", "Person", "Two", "Com", "DDD", "testDDD@probation.gov.uk"),
      UnapprovedLicence("Z265290", "Person", "Three", "Com", "BBB", "testBBB@probation.gov.uk"),
      UnapprovedLicence("A123456", "Person", "Six", "Com", "BBB", "testBBB@probation.gov.uk"),
      UnapprovedLicence("A123457", "Person", "Seven", "Com", "BBB", "testBBB@probation.gov.uk"),
    )
    verify(notifyService).sendUnapprovedLicenceEmail(expectedUnapprovedLicences)
  }
}
