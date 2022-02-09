package uk.gov.justice.digital.hmpps.createandvaryalicenceapi.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.config.ErrorResponse
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.model.UpdateComRequest
import uk.gov.justice.digital.hmpps.createandvaryalicenceapi.repository.LicenceRepository

class OffenderIntegrationTest : IntegrationTestBase() {

  @Autowired
  lateinit var licenceRepository: LicenceRepository

  @Test
  fun `Get forbidden (403) when incorrect roles are supplied`() {
    val requestBody = UpdateComRequest(staffIdentifier = 2000, staffUsername = "joebloggs", staffEmail = "joebloggs@probation.gov.uk")

    val result = webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_WRONG ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectStatus().isEqualTo(HttpStatus.FORBIDDEN.value())
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    assertThat(result?.userMessage).contains("Access is denied")
  }

  @Test
  fun `Unauthorized (401) when no token is supplied`() {
    webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .accept(MediaType.APPLICATION_JSON)
      .exchange()
      .expectStatus().isEqualTo(HttpStatus.UNAUTHORIZED.value())
  }

  @Test
  @Sql(
    "classpath:test_data/seed-licence-id-1.sql"
  )
  fun `Update an offender's inflight licences with new COM details`() {
    val requestBody = UpdateComRequest(staffIdentifier = 3000, staffUsername = "joebloggs", staffEmail = "joebloggs@probation.gov.uk")

    webTestClient.put()
      .uri("/offender/crn/CRN1/responsible-com")
      .bodyValue(requestBody)
      .accept(MediaType.APPLICATION_JSON)
      .headers(setAuthorisation(roles = listOf("ROLE_CVL_ADMIN")))
      .exchange()
      .expectStatus().isOk

    assertThat(licenceRepository.findById(1).get().responsibleCom!!.staffIdentifier).isEqualTo(3000)
    assertThat(licenceRepository.findById(1).get().responsibleCom!!.username).isEqualTo("joebloggs")
    assertThat(licenceRepository.findById(1).get().responsibleCom!!.email).isEqualTo("joebloggs@probation.gov.uk")
  }
}
