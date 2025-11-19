package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.jobs

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.service.NotifyService

class WarnLicenceReviewOverdueIntegrationTest : IntegrationTestBase() {
  @MockitoBean
  lateinit var notifyService: NotifyService

  @Test
  @Sql("classpath:test_data/seed-unreviewed-licences.sql")
  fun `notifies probation of unreviewed licences`() {
    // Given
    val uri = "/jobs/warn-licence-review-overdue"

    // When
    val result = webTestClient.post()
      .uri(uri)
      .accept(MediaType.APPLICATION_JSON)
      .exchange()

    // Then
    result.expectStatus().isOk

    val licenceCrnCaptor = argumentCaptor<String>()
    verify(notifyService, times(4)).sendLicenceReviewOverdueEmail(
      any(),
      any(),
      any(),
      any(),
      licenceCrnCaptor.capture(),
      any(),
      any(),
    )

    assertEquals(
      listOf("U328968", "H598679", "I511234", "Z265290"),
      licenceCrnCaptor.allValues,
    )
  }
}
