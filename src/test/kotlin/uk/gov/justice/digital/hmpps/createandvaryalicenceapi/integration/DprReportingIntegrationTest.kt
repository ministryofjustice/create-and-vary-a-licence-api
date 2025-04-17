package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

private const val GET_REPORT_DEFINITIONS_URL = "/definitions"

@DisplayName("DPR reporting resource tests")
class DprReportingIntegrationTest : IntegrationTestBase() {

  @Nested
  inner class GetDefinitions {
    @Nested
    inner class `report definitions` {
      @Test
      fun `should return forbidden if no role`() {
        webTestClient.get()
          .uri(GET_REPORT_DEFINITIONS_URL)
          .headers(setAuthorisation())
          .exchange()
          .expectStatus()
          .isForbidden
      }

      @Test
      fun `should return forbidden if wrong role`() {
        webTestClient.get()
          .uri(GET_REPORT_DEFINITIONS_URL)
          .headers(setAuthorisation(roles = listOf("ROLE_WRONG")))
          .exchange()
          .expectStatus()
          .isForbidden
      }
      // disabling test until we have a DPD product set up for it to check in resources/dpr_reports
//      @Test
//      fun `returns the definitions of all the reports`() {
//        webTestClient.get().uri(GET_REPORT_DEFINITIONS_URL)
//          .headers(setAuthorisation(roles = listOf("ROLE_NOMIS_BATCHLOAD")))
//          .header("Content-Type", "application/json")
//          .exchange()
//          .expectStatus().isOk
//          .expectBody().jsonPath("$.length()").isEqualTo(1)
//      }
    }
  }
}
